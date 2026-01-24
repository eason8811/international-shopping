package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * PayPal Checkout 事务内准备结果
 *
 * @param paymentId      支付单 ID
 * @param orderId        订单 ID
 * @param orderNo        订单号
 * @param orderCreatedAt 订单创建时间
 * @param currency       币种
 * @param amountMinor    金额 (最小货币单位)
 * @param channel        支付通道 (PAYPAL)
 * @param paymentStatus  支付单状态 (INIT/PENDING)
 * @param paypalOrderId  若已存在 external_id, 则返回该 PayPal Order ID (可空)
 */
public record PayPalCheckoutResult(@NotNull Long paymentId,
                                   @NotNull Long orderId,
                                   @NotNull String orderNo,
                                   @NotNull LocalDateTime orderCreatedAt,
                                   @NotNull String currency,
                                   long amountMinor,
                                   @NotNull PaymentChannel channel,
                                   @NotNull PaymentStatus paymentStatus,
                                   @Nullable String paypalOrderId) {
}
