package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.RefundStatus;

import java.time.LocalDateTime;

/**
 * 管理侧退款单列表展示项 (读模型)
 *
 * @param refundId    退款单 ID
 * @param refundNo    退款单号
 * @param orderId     退款单关联的订单 ID
 * @param orderNo     退款单关联的订单号
 * @param paymentId   支付单 ID
 * @param externalId  退款单外部 ID
 * @param channel     支付渠道
 * @param status      退款单状态
 * @param amountMinor 退款单价格 (Minor 形式)
 * @param currency    币种
 * @param createdAt   创建时间
 * @param updatedAt   更新时间
 */
public record AdminRefundListItemView(@NotNull Long refundId,
                                      @NotNull String refundNo,
                                      @NotNull Long orderId,
                                      @NotNull String orderNo,
                                      @NotNull Long paymentId,
                                      @Nullable String externalId,
                                      @NotNull PaymentChannel channel,
                                      @NotNull RefundStatus status,
                                      long amountMinor,
                                      @NotNull String currency,
                                      @NotNull LocalDateTime createdAt,
                                      @NotNull LocalDateTime updatedAt) {
}
