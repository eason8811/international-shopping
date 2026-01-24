package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Capture 目标信息 (用于事务外调用 PayPal capture)
 *
 * @param paymentId      支付单 ID
 * @param orderId        订单 ID
 * @param orderNo        订单号
 * @param orderStatus    订单状态 (orders.status)
 * @param orderCreatedAt 订单创建时间
 * @param currency       币种
 * @param amountMinor    金额 (最小货币单位)
 * @param paypalOrderId  PayPal Order ID (payment_order.external_id)
 * @param paymentStatus  当前支付单状态
 */
public record CaptureTarget(@NotNull Long paymentId,
                            @NotNull Long orderId,
                            @NotNull String orderNo,
                            @NotNull String orderStatus,
                            @NotNull LocalDateTime orderCreatedAt,
                            @NotNull String currency,
                            long amountMinor,
                            @NotNull String paypalOrderId,
                            @NotNull PaymentStatus paymentStatus) {
}
