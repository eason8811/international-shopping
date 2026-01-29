package shopping.international.domain.service.payment.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.adapter.repository.payment.IPaymentRepository;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.payment.*;
import shopping.international.domain.service.common.impl.CurrencyConfigService;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.types.config.BrandProperties;
import shopping.international.types.config.OrderTimeoutSettings;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 支付领域服务实现 (PayPal)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(BrandProperties.class)
public class PaymentService implements IPaymentService {

    /**
     * 支付领域仓储
     */
    private final IPaymentRepository paymentRepository;
    /**
     * PayPal 支付网关端口
     */
    private final IPayPalPort payPalPort;
    /**
     * 订单超时设置 (用于晚到支付 TTL 判定)
     */
    private final OrderTimeoutSettings orderTimeoutSettings;
    /**
     * @see BrandProperties
     */
    private final BrandProperties brandProperties;
    /**
     * @see CurrencyConfigService
     */
    private final CurrencyConfigService configService;


    /**
     * 创建 PayPal Checkout
     *
     * @param userId         当前用户 ID
     * @param orderNo        订单号
     * @param local          地区代码
     * @param returnUrl      支付成功回跳地址
     * @param cancelUrl      用户取消回跳地址
     * @param idempotencyKey 幂等键 (用于调用 PayPal 创建 Order 的幂等)
     * @return Checkout 响应视图
     */
    @Override
    public @NotNull PayPalCheckoutView createPayPalCheckout(@NotNull Long userId,
                                                            @NotNull String orderNo,
                                                            @Nullable String local,
                                                            @NotNull String returnUrl,
                                                            @NotNull String cancelUrl,
                                                            @NotNull String idempotencyKey) {
        PayPalCheckoutResult checkout = paymentRepository.preparePayPalCheckout(userId, orderNo);
        String paypalOrderId = checkout.paypalOrderId();

        if (paypalOrderId != null && !paypalOrderId.isBlank()) {
            IPayPalPort.GetOrderResult order = payPalPort.getOrder(paypalOrderId);
            return new PayPalCheckoutView(
                    checkout.paymentId(),
                    checkout.orderNo(),
                    PaymentChannel.PAYPAL,
                    checkout.totalAmount(),
                    checkout.currency(),
                    PaymentStatus.PENDING,
                    paypalOrderId,
                    order.approveUrl() != null ? order.approveUrl() : fallbackApproveUrl(paypalOrderId)
            );
        }

        // 并发安全: 使用 "按 paymentId 派生" 的幂等键, 避免并发下重复创建不同 PayPal Order
        String effectiveIdempotencyKey = "ppco-" + checkout.paymentId();

        IPayPalPort.CreateOrderResult created = payPalPort.createOrder(
                IPayPalPort.CreateOrderCommand.builder()
                        .idempotencyKey(effectiveIdempotencyKey)
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .brandName(brandProperties.getBrand())
                        .local(local == null ? "en-US" : local)
                        .shippingPreference("SET_PROVIDED_ADDRESS")
                        .userAction("PAY_NOW")
                        .currency(checkout.currency())
                        .config(configService.get(checkout.currency()))
                        .totalAmount(checkout.totalAmount())
                        .itemTotal(checkout.itemTotal())
                        .shipping(checkout.shipping())
                        .handling(checkout.handling())
                        .taxTotal(checkout.taxTotal())
                        .shippingDiscount(checkout.shippingDiscount())
                        .discount(checkout.discount())
                        .fullName(checkout.fullName())
                        .emailAddress(checkout.emailAddress())
                        .phoneCountryCode(checkout.phoneCountryCode())
                        .phoneNationalNumber(checkout.phoneNationalNumber())
                        .addressLine1(checkout.addressLine1())
                        .addressLine2(checkout.addressLine2())
                        .adminArea2(checkout.adminArea2())
                        .adminArea1(checkout.adminArea1())
                        .postalCode(checkout.postalCode())
                        .countryCode(checkout.countryCode())
                        .build()
        );
        String externalId = created.paypalOrderId();
        try {
            paymentRepository.bindPayPalOrder(checkout.paymentId(), externalId, created.requestJson(), created.responseJson());
        } catch (ConflictException e) {
            // 并发下可能已被其它线程回填/切换有效支付尝试: 回读当前有效 attempt 后再返回
            PayPalCheckoutResult reread = paymentRepository.preparePayPalCheckout(userId, orderNo);
            if (reread.paypalOrderId() == null || reread.paypalOrderId().isBlank())
                throw e;
            IPayPalPort.GetOrderResult order = payPalPort.getOrder(reread.paypalOrderId());
            return new PayPalCheckoutView(
                    reread.paymentId(),
                    reread.orderNo(),
                    PaymentChannel.PAYPAL,
                    reread.totalAmount(),
                    reread.currency(),
                    PaymentStatus.PENDING,
                    reread.paypalOrderId(),
                    order.approveUrl() != null ? order.approveUrl() : fallbackApproveUrl(reread.paypalOrderId())
            );
        }

        return new PayPalCheckoutView(
                checkout.paymentId(),
                checkout.orderNo(),
                PaymentChannel.PAYPAL,
                checkout.totalAmount(),
                checkout.currency(),
                PaymentStatus.PENDING,
                externalId,
                created.approveUrl()
        );
    }

    /**
     * Capture PayPal 支付 (回跳后确认扣款)
     *
     * @param userId         当前用户 ID
     * @param paymentId      支付单 ID
     * @param payerId        可选 payer_id
     * @param note           可选备注
     * @param idempotencyKey 可选幂等键 (用于调用 PayPal capture 的幂等)
     * @return 支付结果视图
     */
    @Override
    public @NotNull PaymentResultView capturePayPalPayment(@NotNull Long userId,
                                                           @NotNull Long paymentId,
                                                           @Nullable String payerId,
                                                           @Nullable String note,
                                                           @Nullable String idempotencyKey) {
        CaptureTarget target = paymentRepository.getCaptureTarget(userId, paymentId);
        if (target.paymentStatus() == PaymentStatus.SUCCESS)
            return new PaymentResultView(target.paymentId(), target.orderNo(), PaymentStatus.SUCCESS, target.paypalOrderId(), "已支付成功 (幂等返回)");

        String effectiveIdempotencyKey = "ppcap-" + target.paymentId();

        IPayPalPort.CaptureOrderResult captured = payPalPort.captureOrder(
                new IPayPalPort.CaptureOrderCommand(
                        effectiveIdempotencyKey,
                        target.paypalOrderId(),
                        payerId,
                        note
                )
        );

        boolean success = isPayPalCaptureSuccess(captured.status());
        LocalDateTime captureTime = toLocalDateTimeOrNow(captured.captureTime());
        PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                new PayPalCaptureApplyCommand(
                        target.paymentId(),
                        target.orderId(),
                        target.paypalOrderId(),
                        success,
                        captureTime,
                        orderTimeoutSettings.ttl(),
                        captured.responseJson(),
                        null,
                        null
                )
        );

        // 晚到支付/已关单支付: 自动退款 (尽量不在同库事务内做外部调用)
        if (success && applied.status() == PaymentStatus.EXCEPTION)
            tryAutoRefundLatePayment(target, captured, note);

        return applied;
    }

    /**
     * 取消本次支付尝试 (关闭支付单, 不等于关闭订单)
     *
     * @param userId    当前用户 ID
     * @param paymentId 支付单 ID
     * @return 支付结果视图
     */
    @Override
    public @NotNull PaymentResultView cancelPayPalPayment(@NotNull Long userId, @NotNull Long paymentId) {
        PaymentResultView view = paymentRepository.cancelPayPalPayment(userId, paymentId);
        return new PaymentResultView(view.paymentId(), view.orderNo(), view.status(), view.externalId(), view.message());
    }

    /**
     * 处理 PayPal Webhook 回调 (匿名入口)
     *
     * @param webhookEvent webhook_event (Map 结构)
     * @param headers      需要参与验签的 header (key 统一使用 "原始 header 名" )
     */
    @Override
    public void handlePayPalWebhook(@NotNull Map<String, Object> webhookEvent, @NotNull Map<String, String> headers) {
        String eventId = String.valueOf(webhookEvent.getOrDefault("id", ""));
        requireNotBlank(eventId, "webhookEvent.id 不能为空");

        Map<String, String> h = normalizeHeaderKeys(headers);
        Duration replayTtl = Duration.ofDays(1);

        payPalPort.verifyWebhookAndReplayProtection(
                new IPayPalPort.VerifyWebhookCommand(
                        requireHeader(h, "PAYPAL-AUTH-ALGO"),
                        requireHeader(h, "PAYPAL-CERT-URL"),
                        requireHeader(h, "PAYPAL-TRANSMISSION-ID"),
                        requireHeader(h, "PAYPAL-TRANSMISSION-SIG"),
                        requireHeader(h, "PAYPAL-TRANSMISSION-TIME"),
                        webhookEvent,
                        eventId,
                        replayTtl
                )
        );

        Optional<String> paypalOrderIdOpt = payPalPort.tryExtractPayPalOrderId(webhookEvent);
        if (paypalOrderIdOpt.isEmpty())
            return;

        String paypalOrderId = paypalOrderIdOpt.get();
        Optional<Long> paymentIdOpt = paymentRepository.findPaymentIdByPayPalOrderId(paypalOrderId);
        if (paymentIdOpt.isEmpty())
            return;

        Long paymentId = paymentIdOpt.get();
        IPayPalPort.GetOrderResult order = payPalPort.getOrder(paypalOrderId);
        paymentRepository.markPolled(paymentId, LocalDateTime.now(), order.responseJson());

        // 仅在检测到 capture 完成时推进 (避免覆盖其它状态)
        if (order.captureTime() == null)
            return;

        CaptureTarget target = paymentRepository.getCaptureTargetForOps(paymentId);
        LocalDateTime captureTime = toLocalDateTimeOrNow(order.captureTime());
        PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                new PayPalCaptureApplyCommand(
                        target.paymentId(),
                        target.orderId(),
                        paypalOrderId,
                        true,
                        captureTime,
                        orderTimeoutSettings.ttl(),
                        order.responseJson(),
                        webhookEvent,
                        LocalDateTime.now()
                )
        );

        // webhook 场景下: 若落库结果为 EXCEPTION（含晚到/关单/非当前有效 attempt）则尝试自动退款
        if (applied.status() == PaymentStatus.EXCEPTION && order.captureId() != null && !order.captureId().isBlank())
            tryAutoRefundLatePayment(target, new IPayPalPort.CaptureOrderResult(
                    paypalOrderId,
                    order.captureId(),
                    order.captureTime(),
                    "COMPLETED",
                    "{}",
                    order.responseJson()
            ), "exception auto refund");
    }

    /**
     * 运维/排障: 强制查询 PayPal 刷新支付状态 (同步一次)
     *
     * @param paymentId 支付单 ID
     */
    @Override
    public void opsSync(@NotNull Long paymentId) {
        CaptureTarget target = paymentRepository.getCaptureTargetForOps(paymentId);
        if (target.paypalOrderId().isBlank())
            return;

        IPayPalPort.GetOrderResult order = payPalPort.getOrder(target.paypalOrderId());
        paymentRepository.markPolled(paymentId, LocalDateTime.now(), order.responseJson());

        if (order.captureTime() == null)
            return;

        LocalDateTime captureTime = toLocalDateTimeOrNow(order.captureTime());
        PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                new PayPalCaptureApplyCommand(
                        target.paymentId(),
                        target.orderId(),
                        target.paypalOrderId(),
                        true,
                        captureTime,
                        orderTimeoutSettings.ttl(),
                        order.responseJson(),
                        null,
                        null
                )
        );

        // opsSync 场景下: 若落库结果为 EXCEPTION（含非当前有效 attempt）则尝试自动退款
        if (applied.status() == PaymentStatus.EXCEPTION && order.captureId() != null && !order.captureId().isBlank())
            tryAutoRefundLatePayment(
                    target,
                    new IPayPalPort.CaptureOrderResult(
                            target.paypalOrderId(),
                            order.captureId(),
                            order.captureTime(),
                            "COMPLETED",
                            "{}",
                            order.responseJson()
                    ),
                    "exception auto refund"
            );
    }

    /**
     * 兜底任务: 扫描并同步非终态支付单
     *
     * @param limit 批次大小
     * @return 本次处理的支付单数量
     */
    @Override
    public int syncNonFinalPayments(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        int processed = 0;
        for (SyncCandidate candidate : paymentRepository.listSyncCandidates(safeLimit)) {
            try {
                if (candidate.paypalOrderId() == null || candidate.paypalOrderId().isBlank())
                    continue;
                opsSync(candidate.paymentId());
                processed++;
            } catch (Exception e) {
                log.warn("支付单兜底同步失败, paymentId={}, err={}", candidate.paymentId(), e.getMessage(), e);
            }
        }
        return processed;
    }

    /**
     * 检查 PayPal 交易状态是否表示成功捕获
     *
     * @param status 交易的状态字符串, 需要为非空
     * @return 如果状态是 {@code "COMPLETED"} 或 {@code "SUCCESS"}, 则返回 <code>true</code>; 否则返回 <code>false</code>
     */
    private static boolean isPayPalCaptureSuccess(@NotNull String status) {
        String s = status.strip().toUpperCase(Locale.ROOT);
        return "COMPLETED".equals(s) || "SUCCESS".equals(s);
    }

    /**
     * 将给定的 <code>OffsetDateTime</code> 转换为 <code>LocalDateTime</code>,
     * 如果输入为 null, 则返回当前时间的 <code>LocalDateTime</code>
     *
     * @param odt 一个可能为空的 {@link OffsetDateTime} 对象
     * @return 如果 <code>odt</code> 不为 null, 返回其对应的 <code>LocalDateTime</code>; 否则, 返回当前时刻的 <code>LocalDateTime</code>
     */
    private static LocalDateTime toLocalDateTimeOrNow(@Nullable OffsetDateTime odt) {
        if (odt == null)
            return LocalDateTime.now();
        return odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * 生成一个备用的批准链接, 当主要支付流程无法使用时, 可以通过此链接进行支付操作
     *
     * @param paypalOrderId PayPal 订单 ID, 用于构建正确的支付链接
     * @return 返回一个字符串, 表示用户可以访问的备用批准 URL
     */
    private static String fallbackApproveUrl(@NotNull String paypalOrderId) {
        return "https://www.paypal.com/checkoutnow?token=" + paypalOrderId;
    }

    /**
     * 尝试对晚到的支付进行自动退款
     *
     * @param target   支付目标, 包含了订单 ID, 支付 ID, 金额等信息
     * @param captured 捕获的订单结果, 包含了captureId等信息
     * @param note     退款时附带的备注信息, 可以为null
     */
    private void tryAutoRefundLatePayment(@NotNull CaptureTarget target,
                                          @NotNull IPayPalPort.CaptureOrderResult captured,
                                          @Nullable String note) {
        if (captured.captureId() == null || captured.captureId().isBlank()) {
            log.warn("晚到支付 captureId 为空自动退款跳过, paymentId: {}, orderNo: {}", target.paymentId(), target.orderNo());
            return;
        }

        String refundNo = RefundNo.generate().getValue();
        IPayPalPort.RefundCaptureResult refunded = payPalPort.refundCapture(
                new IPayPalPort.RefundCaptureCommand(
                        null,
                        captured.captureId(),
                        target.amountMinor(),
                        target.currency(),
                        note
                )
        );

        RefundStatus refundStatus = isPayPalRefundSuccess(refunded.status()) ? RefundStatus.SUCCESS : RefundStatus.PENDING;
        paymentRepository.insertRefund(
                target.orderId(),
                target.paymentId(),
                refundNo,
                refunded.refundId(),
                target.amountMinor(),
                target.currency(),
                refundStatus,
                refunded.requestJson(),
                refunded.responseJson()
        );
    }

    /**
     * 检查给定的状态字符串是否表示 PayPal 退款成功
     *
     * @param status 状态字符串, 应该是不为空的字符串, 表示退款请求的结果状态
     * @return 如果状态字符串为 {@code "COMPLETED"} 或 {@code "SUCCESS"}, 返回 true, 否则返回 false
     */
    private static boolean isPayPalRefundSuccess(@NotNull String status) {
        String s = status.strip().toUpperCase(Locale.ROOT);
        return "COMPLETED".equals(s) || "SUCCESS".equals(s);
    }

    /**
     * 将传入的 headers 中的所有键转换为大写, 并去除首尾空白字符 该方法会忽略键或值为空的条目
     *
     * @param headers 需要被标准化的头部信息 map, 其中 key 是字符串类型的 header 名称, value 是对应的 header 值
     * @return 返回一个新的 map, 包含了处理后的 header 键值对, 所有键都已转换为大写且去除了首尾空白字符
     */
    private static Map<String, String> normalizeHeaderKeys(@NotNull Map<String, String> headers) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toMap(
                        e -> e.getKey().strip().toUpperCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (a, b) -> a
                ));
    }

    /**
     * 从给定的 headers 中获取指定名称的 header 值, 如果该 header 不存在或其值为空白, 则抛出异常
     *
     * @param headers 包含所有请求头信息的映射 map
     * @param name    要查找的 header 名称
     * @return 返回找到的 header 的非空白字符串值
     * @throws IllegalParamException 如果指定的 header 不存在或其值为空白
     */
    private static String requireHeader(@NotNull Map<String, String> headers, @NotNull String name) {
        String v = headers.get(name);
        if (v == null || v.isBlank())
            throw new IllegalParamException("缺少必要 Header: " + name);
        return v.strip();
    }
}
