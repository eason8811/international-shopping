package shopping.international.domain.service.payment.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.adapter.repository.payment.IPaymentRepository;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.payment.*;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.types.config.OrderTimeoutSettings;
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
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 支付领域服务实现 (PayPal)
 */
@Slf4j
@Service
@RequiredArgsConstructor
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
     * 创建 PayPal Checkout
     *
     * @param userId         当前用户 ID
     * @param orderNo        订单号
     * @param returnUrl      支付成功回跳地址
     * @param cancelUrl      用户取消回跳地址
     * @param idempotencyKey 幂等键 (用于调用 PayPal 创建 Order 的幂等)
     * @return Checkout 响应视图
     */
    @Override
    public @NotNull PayPalCheckoutView createPayPalCheckout(@NotNull Long userId,
                                                            @NotNull String orderNo,
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
                    checkout.amountMinor(),
                    checkout.currency(),
                    PaymentStatus.PENDING,
                    paypalOrderId,
                    order.approveUrl() != null ? order.approveUrl() : fallbackApproveUrl(paypalOrderId)
            );
        }

        IPayPalPort.CreateOrderResult created = payPalPort.createOrder(
                new IPayPalPort.CreateOrderCommand(
                        idempotencyKey,
                        returnUrl,
                        cancelUrl,
                        checkout.amountMinor(),
                        checkout.currency()
                ));
        String newPaypalOrderId = created.paypalOrderId();
        paymentRepository.bindPayPalOrder(checkout.paymentId(), newPaypalOrderId, created.requestJson(), created.responseJson());

        return new PayPalCheckoutView(
                checkout.paymentId(),
                checkout.orderNo(),
                PaymentChannel.PAYPAL,
                checkout.amountMinor(),
                checkout.currency(),
                PaymentStatus.PENDING,
                newPaypalOrderId,
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
        requireNotNull(userId, "userId 不能为空");
        requireNotNull(paymentId, "paymentId 不能为空");

        CaptureTarget target = paymentRepository.getCaptureTarget(userId, paymentId);
        if (target.paymentStatus() == PaymentStatus.SUCCESS) {
            return new PaymentResultView(target.paymentId(), PaymentStatus.SUCCESS, target.paypalOrderId(), target.orderNo(), "已支付成功 (幂等返回)");
        }

        IPayPalPort.CaptureOrderResult captured = payPalPort.captureOrder(new IPayPalPort.CaptureOrderCommand(
                idempotencyKey,
                target.paypalOrderId(),
                payerId,
                note
        ));

        boolean success = isPayPalCaptureSuccess(captured.status());
        LocalDateTime captureTime = toLocalDateTimeOrNow(captured.captureTime());

        boolean orderAlreadyClosed = isOrderClosedOrCancelled(target.orderStatus());
        boolean isLate = isLatePayment(target.orderCreatedAt(), captureTime, orderTimeoutSettings.ttl());

        PaymentStatus newPaymentStatus;
        PaymentStatus newOrderPayStatus;
        String newOrderStatus = null;
        LocalDateTime payTime = null;

        if (success && !orderAlreadyClosed && !isLate) {
            newPaymentStatus = PaymentStatus.SUCCESS;
            newOrderPayStatus = PaymentStatus.SUCCESS;
            newOrderStatus = "PAID";
            payTime = captureTime;
        } else if (success) {
            newPaymentStatus = PaymentStatus.EXCEPTION;
            newOrderPayStatus = PaymentStatus.EXCEPTION;
            payTime = captureTime;
        } else {
            newPaymentStatus = PaymentStatus.FAIL;
            newOrderPayStatus = PaymentStatus.FAIL;
        }

        shopping.international.domain.model.vo.payment.PaymentResultView applied = paymentRepository.txApplyCaptureResult(new CaptureApplyCommand(
                target.paymentId(),
                target.orderId(),
                target.orderNo(),
                PaymentChannel.PAYPAL,
                newPaymentStatus,
                target.paypalOrderId(),
                payTime,
                captured.responseJson(),
                null,
                null,
                newOrderStatus,
                newOrderPayStatus
        ));

        // 晚到支付/已关单支付: 自动退款 (尽量不在同库事务内做外部调用)
        if (success && (orderAlreadyClosed || isLate)) {
            tryAutoRefundLatePayment(target, captured, note);
        }

        return new PaymentResultView(applied.paymentId(), applied.status(), applied.externalId(), applied.orderNo(), applied.message());
    }

    /**
     * 取消本次支付尝试 (关闭支付单，不等于关闭订单)
     *
     * @param userId    当前用户 ID
     * @param paymentId 支付单 ID
     * @return 支付结果视图
     */
    @Override
    public @NotNull PaymentResultView cancelPayPalPayment(@NotNull Long userId, @NotNull Long paymentId) {
        requireNotNull(userId, "userId 不能为空");
        requireNotNull(paymentId, "paymentId 不能为空");
        shopping.international.domain.model.vo.payment.PaymentResultView view = paymentRepository.txCancelPayPalPayment(userId, paymentId);
        return new PaymentResultView(view.paymentId(), view.status(), view.externalId(), view.orderNo(), view.message());
    }

    /**
     * 处理 PayPal Webhook 回调 (匿名入口)
     *
     * @param webhookEvent webhook_event (Map 结构)
     * @param headers      需要参与验签的 header (key 统一使用 "原始 header 名" )
     */
    @Override
    public void handlePayPalWebhook(@NotNull Map<String, Object> webhookEvent, @NotNull Map<String, String> headers) {
        requireNotNull(webhookEvent, "webhookEvent 不能为空");
        requireNotNull(headers, "headers 不能为空");

        String eventId = String.valueOf(webhookEvent.getOrDefault("id", ""));
        requireNotBlank(eventId, "webhookEvent.id 不能为空");

        Map<String, String> h = normalizeHeaderKeys(headers);
        Duration replayTtl = Duration.ofDays(1);

        payPalPort.verifyWebhookAndReplayProtection(new IPayPalPort.VerifyWebhookCommand(
                requireHeader(h, "PAYPAL-AUTH-ALGO"),
                requireHeader(h, "PAYPAL-CERT-URL"),
                requireHeader(h, "PAYPAL-TRANSMISSION-ID"),
                requireHeader(h, "PAYPAL-TRANSMISSION-SIG"),
                requireHeader(h, "PAYPAL-TRANSMISSION-TIME"),
                webhookEvent,
                eventId,
                replayTtl
        ));

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
        boolean orderAlreadyClosed = isOrderClosedOrCancelled(target.orderStatus());
        boolean isLate = isLatePayment(target.orderCreatedAt(), captureTime, orderTimeoutSettings.ttl());

        PaymentStatus newPaymentStatus = (orderAlreadyClosed || isLate) ? PaymentStatus.EXCEPTION : PaymentStatus.SUCCESS;
        PaymentStatus newOrderPayStatus = (orderAlreadyClosed || isLate) ? PaymentStatus.EXCEPTION : PaymentStatus.SUCCESS;
        String newOrderStatus = (!orderAlreadyClosed && !isLate) ? "PAID" : null;

        paymentRepository.txApplyCaptureResult(new CaptureApplyCommand(
                target.paymentId(),
                target.orderId(),
                target.orderNo(),
                PaymentChannel.PAYPAL,
                newPaymentStatus,
                paypalOrderId,
                captureTime,
                order.responseJson(),
                webhookEvent,
                LocalDateTime.now(),
                newOrderStatus,
                newOrderPayStatus
        ));

        if (orderAlreadyClosed || isLate) {
            // webhook 场景下可尝试自动退款: 若能从 getOrder 中拿到 captureId 则走退款
            if (order.captureId() != null && !order.captureId().isBlank()) {
                tryAutoRefundLatePayment(target, new IPayPalPort.CaptureOrderResult(
                        paypalOrderId,
                        order.captureId(),
                        order.captureTime(),
                        "COMPLETED",
                        "{}",
                        order.responseJson()
                ), "late payment auto refund");
            }
        }
    }

    /**
     * 运维/排障: 强制查询 PayPal 刷新支付状态 (同步一次)
     *
     * @param paymentId 支付单 ID
     */
    @Override
    public void opsSync(@NotNull Long paymentId) {
        requireNotNull(paymentId, "paymentId 不能为空");
        CaptureTarget target = paymentRepository.getCaptureTargetForOps(paymentId);
        if (target.paypalOrderId().isBlank())
            return;

        IPayPalPort.GetOrderResult order = payPalPort.getOrder(target.paypalOrderId());
        paymentRepository.markPolled(paymentId, LocalDateTime.now(), order.responseJson());

        if (order.captureTime() == null)
            return;

        LocalDateTime captureTime = toLocalDateTimeOrNow(order.captureTime());
        boolean orderAlreadyClosed = isOrderClosedOrCancelled(target.orderStatus());
        boolean isLate = isLatePayment(target.orderCreatedAt(), captureTime, orderTimeoutSettings.ttl());

        PaymentStatus newPaymentStatus = (orderAlreadyClosed || isLate) ? PaymentStatus.EXCEPTION : PaymentStatus.SUCCESS;
        PaymentStatus newOrderPayStatus = (orderAlreadyClosed || isLate) ? PaymentStatus.EXCEPTION : PaymentStatus.SUCCESS;
        String newOrderStatus = (!orderAlreadyClosed && !isLate) ? "PAID" : null;

        paymentRepository.txApplyCaptureResult(new CaptureApplyCommand(
                target.paymentId(),
                target.orderId(),
                target.orderNo(),
                PaymentChannel.PAYPAL,
                newPaymentStatus,
                target.paypalOrderId(),
                captureTime,
                order.responseJson(),
                null,
                null,
                newOrderStatus,
                newOrderPayStatus
        ));
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
     * 判断支付是否逾期
     *
     * @param orderCreatedAt 订单创建的时间, 不可为空
     * @param captureTime    实际支付的时间, 不可为空
     * @param ttl            从订单创建到必须完成支付的时间期限, 不可为空
     * @return 如果支付时间晚于订单创建时间加上 ttl, 返回 true, 否则返回 false
     */
    private static boolean isLatePayment(@NotNull LocalDateTime orderCreatedAt, @NotNull LocalDateTime captureTime, @NotNull Duration ttl) {
        LocalDateTime ddl = orderCreatedAt.plus(ttl);
        return captureTime.isAfter(ddl);
    }

    /**
     * 检查给定的订单状态是否为已关闭或已取消
     *
     * @param orderStatus 订单的状态, 不能为空
     * @return 如果订单状态是 "CANCELLED" 或 "CLOSED", 则返回 true; 否则返回 false
     */
    private static boolean isOrderClosedOrCancelled(@NotNull String orderStatus) {
        String s = orderStatus.strip().toUpperCase(Locale.ROOT);
        return "CANCELLED".equals(s) || "CLOSED".equals(s);
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
            log.warn("晚到支付自动退款跳过: captureId 为空, paymentId: {}, orderNo: {}", target.paymentId(), target.orderNo());
            return;
        }

        String refundNo = RefundNo.generate().getValue();
        IPayPalPort.RefundCaptureResult refunded = payPalPort.refundCapture(new IPayPalPort.RefundCaptureCommand(
                null,
                captured.captureId(),
                target.amountMinor(),
                target.currency(),
                note
        ));

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
