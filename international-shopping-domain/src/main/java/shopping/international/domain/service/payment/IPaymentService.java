package shopping.international.domain.service.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;

import java.util.Map;

/**
 * 用户侧支付领域服务接口
 *
 * <p>覆盖用户侧支付用例: </p>
 * <ul>
 *     <li>创建/复用支付单并生成 PayPal Checkout 跳转链接</li>
 *     <li>回跳后 capture 确认扣款 (幂等)</li>
 *     <li>取消本次支付尝试 (关闭支付单)</li>
 *     <li>接收并处理 PayPal Webhook (验签 + 防重放 + 幂等)</li>
 * </ul>
 */
public interface IPaymentService {

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
    @NotNull
    PayPalCheckoutView createPayPalCheckout(@NotNull Long userId,
                                            @NotNull String orderNo,
                                            @NotNull String returnUrl,
                                            @NotNull String cancelUrl,
                                            @NotNull String idempotencyKey);

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
    @NotNull
    PaymentResultView capturePayPalPayment(@NotNull Long userId,
                                           @NotNull Long paymentId,
                                           @Nullable String payerId,
                                           @Nullable String note,
                                           @Nullable String idempotencyKey);

    /**
     * 取消本次支付尝试 (关闭支付单，不等于关闭订单)
     *
     * @param userId    当前用户 ID
     * @param paymentId 支付单 ID
     * @return 支付结果视图
     */
    @NotNull
    PaymentResultView cancelPayPalPayment(@NotNull Long userId, @NotNull Long paymentId);

    /**
     * 处理 PayPal Webhook 回调 (匿名入口)
     *
     * @param webhookEvent webhook_event (Map 结构)
     * @param headers      需要参与验签的 header (key 统一使用 "原始 header 名" )
     */
    void handlePayPalWebhook(@NotNull Map<String, Object> webhookEvent, @NotNull Map<String, String> headers);

    /**
     * 运维/排障: 强制查询 PayPal 刷新支付状态 (同步一次)
     *
     * @param paymentId 支付单 ID
     */
    void opsSync(@NotNull Long paymentId);

    /**
     * 兜底任务: 扫描并同步非终态支付单
     *
     * @param limit 批次大小
     * @return 本次处理的支付单数量
     */
    int syncNonFinalPayments(int limit);

    /**
     * PayPal Checkout 结果视图
     *
     * @param paymentId     支付单 ID
     * @param orderNo       订单号
     * @param channel       支付渠道
     * @param amountMinor   支付金额
     * @param currency      币种
     * @param status        当前支付的状态, 参见 {@link PaymentStatus} 枚举
     * @param paypalOrderId PayPal生成的订单ID, 用于在PayPal系统中跟踪此次支付
     * @param approveUrl    用户需要访问的URL, 以便完成支付流程
     */
    record PayPalCheckoutView(@NotNull Long paymentId,
                              @NotNull String orderNo,
                              @NotNull PaymentChannel channel,
                              long amountMinor,
                              @NotNull String currency,
                              @NotNull PaymentStatus status,
                              @NotNull String paypalOrderId,
                              @NotNull String approveUrl) {
    }

    /**
     * 支付结果视图
     *
     * @param paymentId  支付单 ID
     * @param status     当前支付的状态, 参见 {@link PaymentStatus} 枚举
     * @param externalId 外部 ID
     * @param orderNo    订单号
     * @param message    消息
     */
    record PaymentResultView(@NotNull Long paymentId,
                             @NotNull PaymentStatus status,
                             @Nullable String externalId,
                             @Nullable String orderNo,
                             @Nullable String message) {
    }
}

