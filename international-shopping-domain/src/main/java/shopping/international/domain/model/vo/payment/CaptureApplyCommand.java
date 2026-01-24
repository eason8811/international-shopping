package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 应用 capture 结果命令 (由领域服务根据 PayPal 返回与晚到支付规则计算后组装)
 *
 * @param paymentId         支付单 ID
 * @param orderId           订单 ID
 * @param orderNo           订单号
 * @param channel           支付通道
 * @param newPaymentStatus  支付单新状态
 * @param paypalOrderId     PayPal Order ID
 * @param payTime           支付时间 (可空)
 * @param responsePayload   捕获响应报文 (JSON, 可空)
 * @param notifyPayload     回调报文 (JSON, 可空)
 * @param lastNotifiedAt    回调处理时间 (可空)
 * @param newOrderStatus    orders.status 要推进到的状态 (可空: 表示不变更订单状态)
 * @param newOrderPayStatus orders.pay_status 要写入的冗余支付状态 (必填)
 */
public record CaptureApplyCommand(@NotNull Long paymentId,
                                  @NotNull Long orderId,
                                  @NotNull String orderNo,
                                  @NotNull PaymentChannel channel,
                                  @NotNull PaymentStatus newPaymentStatus,
                                  @NotNull String paypalOrderId,
                                  @Nullable LocalDateTime payTime,
                                  @Nullable String responsePayload,
                                  @Nullable Object notifyPayload,
                                  @Nullable LocalDateTime lastNotifiedAt,
                                  @Nullable String newOrderStatus,
                                  @NotNull PaymentStatus newOrderPayStatus) {
}
