package shopping.international.domain.service.payment.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.adapter.repository.payment.IPaymentRepository;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.enums.payment.paypal.PayPalCaptureStatus;
import shopping.international.domain.model.enums.payment.paypal.PayPalOrderStatus;
import shopping.international.domain.model.enums.payment.paypal.PayPalRefundStatus;
import shopping.international.domain.model.vo.payment.*;
import shopping.international.domain.service.common.impl.CurrencyConfigService;
import shopping.international.domain.service.payment.IPaymentService;
import shopping.international.types.config.BrandProperties;
import shopping.international.types.config.OrderTimeoutSettings;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.PayPalException;
import shopping.international.types.exceptions.PayPalWebhookReplayException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.nestedString;
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

        PayPalCaptureStatus captureStatus = captured.captureStatus();
        if (captureStatus != null) {
            if (captureStatus == PayPalCaptureStatus.PENDING) {
                paymentRepository.markPolled(target.paymentId(), LocalDateTime.now(), captured.responseJson(), captured.captureId());
                return new PaymentResultView(target.paymentId(), target.orderNo(), PaymentStatus.PENDING, target.paypalOrderId(), "PayPal capture 状态 PENDING");
            }
            // REFUNDED / PARTIALLY_REFUNDED: 交给退款流程处理 (此处仅记录响应用于排障)
            if (captureStatus == PayPalCaptureStatus.REFUNDED || captureStatus == PayPalCaptureStatus.PARTIALLY_REFUNDED) {
                paymentRepository.markPolled(target.paymentId(), LocalDateTime.now(), captured.responseJson(), captured.captureId());
                return new PaymentResultView(target.paymentId(), target.orderNo(), PaymentStatus.PENDING, target.paypalOrderId(), "PayPal capture 已进入退款相关状态: " + captureStatus);
            }

            LocalDateTime captureTime = toLocalDateTimeOrNow(captured.captureTime());
            boolean captureSuccess = captureStatus == PayPalCaptureStatus.COMPLETED;
            PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                    new PayPalCaptureApplyCommand(
                            target.paymentId(),
                            target.orderId(),
                            target.paypalOrderId(),
                            captureSuccess,
                            captured.captureId(),
                            captureTime,
                            orderTimeoutSettings.ttl(),
                            captured.responseJson(),
                            null,
                            null,
                            OrderStatusEventSource.USER,
                            "用户回跳系统订单页面, captureStatus 为: " + captureStatus
                    )
            );

            // 晚到支付/已关单支付: 自动退款 (尽量不在同库事务内做外部调用)
            if (captureSuccess && applied.status() == PaymentStatus.EXCEPTION)
                tryAutoRefundLatePayment(target, captured, "接口调用 capture 支付单状态为 EXCEPTION 自动退款");

            return applied;
        }

        PayPalOrderStatus orderStatus = captured.orderStatus();
        if (orderStatus == PayPalOrderStatus.VOIDED)
            return cancelPayPalPayment(userId, paymentId);

        paymentRepository.markPolled(target.paymentId(), LocalDateTime.now(), captured.responseJson(), captured.captureId());
        return new PaymentResultView(target.paymentId(), target.orderNo(), PaymentStatus.PENDING, target.paypalOrderId(), "PayPal capture.status 不可判定, order.status=" + orderStatus);
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
        Duration replayTtl = Duration.ofDays(4);

        try {
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
        } catch (PayPalWebhookReplayException e) {
            log.info("PayPal Webhook 重复事件忽略, eventId:{}, msg: {}", eventId, e.getMessage());
            return;
        }

        Optional<String> paypalOrderIdOpt = payPalPort.tryExtractPayPalOrderId(webhookEvent);
        if (paypalOrderIdOpt.isEmpty())
            return;

        String paypalOrderId = paypalOrderIdOpt.get();
        Optional<Long> paymentIdOpt = paymentRepository.findPaymentIdByPayPalOrderId(paypalOrderId);
        if (paymentIdOpt.isEmpty())
            return;

        String eventType = String.valueOf(webhookEvent.getOrDefault("event_type", "")).strip();
        if (eventType.isBlank())
            return;
        String upperEventType = eventType.toUpperCase(Locale.ROOT);

        Long paymentId = paymentIdOpt.get();
        CaptureTarget target;
        try {
            target = paymentRepository.getCaptureTargetForOps(paymentId);
        } catch (Exception e) {
            log.warn("PayPal Webhook 命中支付单但不允许处理, eventType={}, eventId={}, paymentId={}, err={}",
                    eventType, eventId, paymentId, e.getMessage());
            return;
        }

        // 订阅事件:
        // 1) CHECKOUT.ORDER.APPROVED: 触发一次 capture (幂等键 ppcap-{paymentId})
        switch (upperEventType) {
            case "CHECKOUT.ORDER.APPROVED" -> handlePayPalCheckoutOrderApproved(target, webhookEvent);


            // 2) PAYMENT.CAPTURE.COMPLETED: capture 入账完成, 推进支付单/订单支付状态为 SUCCESS
            case "PAYMENT.CAPTURE.COMPLETED" ->
                    handlePayPalCaptureWebhook(target, webhookEvent, PayPalCaptureStatus.COMPLETED);


            // 3) PAYMENT.CAPTURE.DECLINED/DENIED: 支付失败, 推进为 FAIL
            case "PAYMENT.CAPTURE.DECLINED", "PAYMENT.CAPTURE.DENIED" ->
                    handlePayPalCaptureWebhook(target, webhookEvent, PayPalCaptureStatus.DECLINED);


            // 4) PAYMENT.CAPTURE.REFUNDED/REVERSED: 资金退款/撤销/冲正, 进入退款对账逻辑
            case "PAYMENT.CAPTURE.REFUNDED", "PAYMENT.CAPTURE.REVERSED" ->
                    handlePayPalCaptureRefundWebhook(target, webhookEvent);
        }
    }

    /**
     * 处理 PayPal 订单批准后的检查流程
     *
     * <p>该方法主要用于在接收到 PayPal 的 {@code CHECKOUT.ORDER.APPROVED} 事件后, 对订单进行捕获并更新数据库中相应的支付状态信息
     * 根据捕获结果的不同, 可能会执行关闭支付操作或尝试自动退款等额外处理
     *
     * @param target       指定的捕获目标, 包含支付 ID 和 PayPal 订单 ID 等必要信息 {@link CaptureTarget}
     * @param webhookEvent 来自 PayPal 的 Webhook 事件数据, 以键值对形式存储
     * @throws NullPointerException 如果传入的参数为 null
     */
    private void handlePayPalCheckoutOrderApproved(@NotNull CaptureTarget target, @NotNull Map<String, Object> webhookEvent) {
        IPayPalPort.CaptureOrderResult captured = payPalPort.captureOrder(
                new IPayPalPort.CaptureOrderCommand(
                        "ppcap-" + target.paymentId(),
                        target.paypalOrderId(),
                        null,
                        "webhook CHECKOUT.ORDER.APPROVED capture"
                )
        );
        paymentRepository.markPolled(target.paymentId(), LocalDateTime.now(), captured.responseJson(), captured.captureId());

        PayPalOrderStatus orderStatus = captured.orderStatus();
        if (orderStatus == PayPalOrderStatus.VOIDED) {
            paymentRepository.closePayPalPaymentForOps(target.paymentId());
            return;
        }

        PayPalCaptureStatus capturedStatus = captured.captureStatus();
        if (capturedStatus == null || capturedStatus == PayPalCaptureStatus.PENDING
                || capturedStatus == PayPalCaptureStatus.REFUNDED || capturedStatus == PayPalCaptureStatus.PARTIALLY_REFUNDED)
            return;

        LocalDateTime captureTime = toLocalDateTimeOrNow(captured.captureTime());
        boolean captureSuccess = capturedStatus == PayPalCaptureStatus.COMPLETED;
        PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                new PayPalCaptureApplyCommand(
                        target.paymentId(),
                        target.orderId(),
                        target.paypalOrderId(),
                        captureSuccess,
                        captured.captureId(),
                        captureTime,
                        orderTimeoutSettings.ttl(),
                        captured.responseJson(),
                        webhookEvent,
                        LocalDateTime.now(),
                        OrderStatusEventSource.PAYMENT_CALLBACK,
                        "接收到 CHECKOUT.ORDER.APPROVED 事件, capturedStatus 为: " + capturedStatus
                )
        );

        if (captureSuccess && applied.status() == PaymentStatus.EXCEPTION)
            tryAutoRefundLatePayment(target, captured, "APPROVED WebHook 回调后支付单状态为 EXCEPTION 自动退款");
    }

    /**
     * 处理 PayPal 捕获 {@code PAYMENT.CAPTURE.COMPLETED/DECLINED/DENIED} 事件的 webhook 通知
     *
     * @param target         交易目标 包含支付 ID, 订单 ID 和 PayPal 订单 ID {@link CaptureTarget}
     * @param webhookEvent   Webhook 事件数据, 一个键值对映射, 其中包含了捕获操作的相关信息
     * @param expectedStatus 预期的捕获状态, 用于验证捕获是否按预期完成
     */
    private void handlePayPalCaptureWebhook(@NotNull CaptureTarget target,
                                            @NotNull Map<String, Object> webhookEvent,
                                            @NotNull PayPalCaptureStatus expectedStatus) {
        String captureId = nestedString(webhookEvent, "resource", "id");
        String updateTimeString = nestedString(webhookEvent, "resource", "update_time");
        String createTimeString = nestedString(webhookEvent, "resource", "create_time");
        OffsetDateTime offsetDateTime = null;
        try {
            if (updateTimeString != null || createTimeString != null)
                offsetDateTime = OffsetDateTime.parse(updateTimeString != null ? updateTimeString : createTimeString);
        } catch (Exception e) {
            log.warn("Capture 回调时间转换失败, create_time: {}, update_time: {}", createTimeString, updateTimeString);
        }
        LocalDateTime captureTime = toLocalDateTimeOrNow(offsetDateTime);
        if (captureId == null || captureTime == null)
            return;

        boolean captureSuccess = expectedStatus == PayPalCaptureStatus.COMPLETED;
        PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                new PayPalCaptureApplyCommand(
                        target.paymentId(),
                        target.orderId(),
                        target.paypalOrderId(),
                        captureSuccess,
                        captureId,
                        captureTime,
                        orderTimeoutSettings.ttl(),
                        null,
                        webhookEvent,
                        LocalDateTime.now(),
                        OrderStatusEventSource.PAYMENT_CALLBACK,
                        "接收到 PAYMENT.CAPTURE.COMPLETED/DECLINED/DENIED 事件, captureStatus 为: " + expectedStatus
                )
        );

        if (captureSuccess && applied.status() == PaymentStatus.EXCEPTION && !captureId.isBlank()) {
            tryAutoRefundLatePayment(
                    target,
                    new IPayPalPort.CaptureOrderResult(
                            target.paypalOrderId(),
                            captureId,
                            offsetDateTime,
                            PayPalOrderStatus.COMPLETED,
                            PayPalCaptureStatus.COMPLETED,
                            "{}",
                            "{}"
                    ),
                    "CAPTURE WebHook 回调后支付单状态为 EXCEPTION 自动退款"
            );
        }
    }

    /**
     * 处理来自 PayPal 的捕获退款 webhook 事件
     *
     * @param target       捕获目标, 包含订单 ID 和支付 ID 等信息
     * @param webhookEvent Webhook 事件数据, 是一个键值对的映射, 其中包含退款相关的详细信息
     */
    private void handlePayPalCaptureRefundWebhook(@NotNull CaptureTarget target, @NotNull Map<String, Object> webhookEvent) {
        // Webhook: 尽量命中本地退款单更新状态/notifyPayload, 若未命中则补插一条退款事实表用于对账
        String currency = Objects.requireNonNullElse(nestedString(webhookEvent, "resource", "amount", "currency_code"), target.currency());
        String value = nestedString(webhookEvent, "resource", "amount", "value");
        String externalRefundId = nestedString(webhookEvent, "resource", "id");
        String paypalStatus = nestedString(webhookEvent, "resource", "status");
        if (externalRefundId == null)
            throw new PayPalException("收到的退款 WebHook 回调请求体中没有包含必须的 external_refund_id ($.resource.id)");

        Long amountMinor = null;
        if (!currency.isBlank() && value != null && !value.isBlank())
            try {
                amountMinor = configService.get(currency).toMinorRounded(new BigDecimal(value));
            } catch (Exception ignore) {
            }
        if (amountMinor == null)
            amountMinor = target.amountMinor();

        RefundTarget refundTarget = paymentRepository.getRefundTargetForWebhook(
                new PayPalRefundWebhookCommand(
                        target.orderId(),
                        target.paymentId(),
                        RefundNo.generate().getValue(),
                        externalRefundId,
                        amountMinor,
                        currency.isBlank() ? target.currency() : currency,
                        mapPayPalRefundStatus(paypalStatus),
                        webhookEvent,
                        LocalDateTime.now()
                )
        );
        paymentRepository.applyRefundResult(
                refundTarget,
                new IPayPalPort.GetRefundResult(externalRefundId, paypalStatus == null ? "" : paypalStatus, "{}"),
                webhookEvent,
                mapPayPalRefundStatus(paypalStatus),
                OrderStatusEventSource.PAYMENT_CALLBACK,
                "refund webhook"
        );
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
        paymentRepository.markPolled(paymentId, LocalDateTime.now(), order.responseJson(), order.captureId());

        PayPalCaptureStatus captureStatus = order.captureStatus();
        if (captureStatus != null) {
            if (captureStatus == PayPalCaptureStatus.PENDING
                    || captureStatus == PayPalCaptureStatus.REFUNDED || captureStatus == PayPalCaptureStatus.PARTIALLY_REFUNDED) {
                paymentRepository.markPolled(target.paymentId(), LocalDateTime.now(), order.responseJson(), order.captureId());
                return;
            }

            LocalDateTime captureTime = toLocalDateTimeOrNow(order.captureTime());
            boolean captureSuccess = captureStatus == PayPalCaptureStatus.COMPLETED;
            PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                    new PayPalCaptureApplyCommand(
                            target.paymentId(),
                            target.orderId(),
                            target.paypalOrderId(),
                            captureSuccess,
                            order.captureId(),
                            captureTime,
                            orderTimeoutSettings.ttl(),
                            order.responseJson(),
                            null,
                            null,
                            OrderStatusEventSource.SCHEDULER,
                            "兜底定时任务, captureStatus 为: " + captureStatus
                    )
            );

            // opsSync 场景下: 若落库结果为 EXCEPTION (含非当前有效 attempt) 则尝试自动退款
            if (captureSuccess && applied.status() == PaymentStatus.EXCEPTION)
                tryAutoRefundLatePayment(
                        target,
                        new IPayPalPort.CaptureOrderResult(
                                target.paypalOrderId(),
                                order.captureId(),
                                order.captureTime(),
                                order.orderStatus(),
                                order.captureStatus(),
                                "{}",
                                order.responseJson()
                        ),
                        "兜底同步支付单状态为 EXCEPTION 自动退款"
                );
            return;
        }

        PayPalOrderStatus orderStatus = order.orderStatus();
        if (orderStatus == PayPalOrderStatus.VOIDED) {
            paymentRepository.closePayPalPaymentForOps(paymentId);
            return;
        }

        if (orderStatus != PayPalOrderStatus.APPROVED)
            return;

        // ORDER.APPROVED 且未出现 capture: 补打一遍 capture (幂等键 ppcap-{paymentId}) 后按 capture.status 分支处理
        IPayPalPort.CaptureOrderResult captured = payPalPort.captureOrder(
                new IPayPalPort.CaptureOrderCommand(
                        "ppcap-" + paymentId,
                        target.paypalOrderId(),
                        null,
                        "opsSync capture"
                )
        );
        paymentRepository.markPolled(paymentId, LocalDateTime.now(), captured.responseJson(), captured.captureId());

        PayPalCaptureStatus capturedStatus = captured.captureStatus();
        if (capturedStatus == null || capturedStatus == PayPalCaptureStatus.PENDING
                || capturedStatus == PayPalCaptureStatus.REFUNDED || capturedStatus == PayPalCaptureStatus.PARTIALLY_REFUNDED)
            return;

        LocalDateTime captureTime = toLocalDateTimeOrNow(captured.captureTime());
        boolean captureSuccess = capturedStatus == PayPalCaptureStatus.COMPLETED;
        PaymentResultView applied = paymentRepository.applyPayPalCaptureResult(
                new PayPalCaptureApplyCommand(
                        target.paymentId(),
                        target.orderId(),
                        target.paypalOrderId(),
                        captureSuccess,
                        captured.captureId(),
                        captureTime,
                        orderTimeoutSettings.ttl(),
                        captured.responseJson(),
                        null,
                        null,
                        OrderStatusEventSource.SCHEDULER,
                        "兜底定时任务, 补打了一遍 capture 接口, captureStatus 为: " + capturedStatus
                )
        );

        if (captureSuccess && applied.status() == PaymentStatus.EXCEPTION)
            tryAutoRefundLatePayment(target, captured, "兜底同步支付单状态为 EXCEPTION 自动退款");
    }

    /**
     * 兜底同步: 查询 PayPal 刷新退款状态 (同步一次)
     *
     * @param refundId 退款单 ID
     */
    private void syncSingleRefund(@NotNull Long refundId) {
        RefundTarget target = paymentRepository.getRefundTargetForOps(refundId);
        if (target.externalRefundId().isBlank())
            return;
        IPayPalPort.GetRefundResult refund = payPalPort.getRefund(target.externalRefundId());
        paymentRepository.markRefundPolled(refundId, LocalDateTime.now(), refund.responseJson());

        RefundStatus refundStatus = mapPayPalRefundStatus(refund.status());
        if (refundStatus == RefundStatus.PENDING)
            return;
        paymentRepository.applyRefundResult(
                target,
                refund,
                Collections.emptyMap(),
                refundStatus,
                OrderStatusEventSource.SCHEDULER,
                "refund sync (polled SUCCESS)"
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
                log.warn("支付单兜底同步失败, paymentId: {}, err: {}", candidate.paymentId(), e.getMessage(), e);
            }
        }
        return processed;
    }

    /**
     * 兜底任务: 扫描并同步非终态退款单
     *
     * <p>用于兜底处理以下场景:</p>
     * <ul>
     *     <li>网关退款返回 PENDING, 回调未到或未成功处理</li>
     *     <li>运维补偿/重试时需要刷新退款状态并推进订单/库存</li>
     * </ul>
     *
     * @param limit 单批最大数量
     * @return 本次处理的退款单数量
     */
    @Override
    public int syncNonFinalRefunds(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        int processed = 0;
        for (RefundSyncCandidate candidate : paymentRepository.listRefundSyncCandidates(safeLimit)) {
            try {
                if (candidate.paypalRefundId() == null || candidate.paypalRefundId().isBlank())
                    continue;
                syncSingleRefund(candidate.refundId());
                processed++;
            } catch (Exception e) {
                log.warn("退款单兜底同步失败, paymentId: {}, err: {}", candidate.refundId(), e.getMessage(), e);
            }
        }
        return processed;
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

        String clientRefundNo = "ppref-" + target.paymentId();
        if (paymentRepository.existsRefundDedupeKey(target.paymentId(), clientRefundNo)) {
            log.info("自动退款去重命中, 已存在退款记录跳过, paymentId: {}, orderNo: {}, clientRefundNo: {}",
                    target.paymentId(), target.orderNo(), clientRefundNo);
            return;
        }

        String refundNo = RefundNo.generate().getValue();
        IPayPalPort.RefundCaptureResult refunded = payPalPort.refundCapture(
                new IPayPalPort.RefundCaptureCommand(
                        clientRefundNo,
                        captured.captureId(),
                        target.amountMinor(),
                        target.currency(),
                        note
                )
        );

        RefundStatus refundStatus = mapPayPalRefundStatus(refunded.status());
        String externalRefundId = refunded.refundId();
        Long newRefundOrderId = paymentRepository.insertRefund(
                target.orderId(),
                target.paymentId(),
                refundNo,
                externalRefundId,
                clientRefundNo,
                target.amountMinor(),
                target.currency(),
                refundStatus,
                refunded.requestJson(),
                refunded.responseJson()
        );

        if (externalRefundId == null || externalRefundId.isBlank())
            throw new PayPalException("自动退款失败, orderId: " + target.orderId() + ", paymentOrderId: " + target.paypalOrderId());

        paymentRepository.applyRefundResult(
                new RefundTarget(newRefundOrderId, target.orderId(), externalRefundId, target.paymentId(), refundStatus),
                new IPayPalPort.GetRefundResult(externalRefundId, refunded.status(), refunded.responseJson()),
                Collections.emptyMap(),
                refundStatus,
                OrderStatusEventSource.SYSTEM,
                "支付单状态异常, 自动退款, 详细信息: ‘" + note + "'"
        );
    }

    /**
     * 将 PayPal 的退款状态映射到系统内部的 <code>RefundStatus</code>
     *
     * @param paypalStatus 从 PayPal 接收到的退款状态字符串, 可能为 null 或空白
     * @return 对应于 PayPal 状态的 <code>RefundStatus</code> 枚举值 如果输入为空或未识别, 默认返回 <code>PENDING</code>
     */
    private static RefundStatus mapPayPalRefundStatus(@Nullable String paypalStatus) {
        PayPalRefundStatus s = PayPalRefundStatus.from(paypalStatus);
        return switch (s) {
            case COMPLETED -> RefundStatus.SUCCESS;
            case PENDING, UNKNOWN -> RefundStatus.PENDING;
            case FAILED, CANCELLED -> RefundStatus.FAIL;
        };
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
