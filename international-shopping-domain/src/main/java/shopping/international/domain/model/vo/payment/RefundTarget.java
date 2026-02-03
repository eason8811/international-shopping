package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.payment.RefundStatus;

/**
 * Refund 目标信息
 *
 * @param refundId         退款单 ID
 * @param orderId          退款单所属的订单 ID
 * @param externalRefundId 外部退款单 ID
 * @param paymentOrderId   关联的支付单 ID
 * @param status           状态
 */
public record RefundTarget(@NotNull Long refundId,
                           @NotNull Long orderId,
                           @NotNull String externalRefundId,
                           @NotNull Long paymentOrderId,
                           @NotNull RefundStatus status) {
}
