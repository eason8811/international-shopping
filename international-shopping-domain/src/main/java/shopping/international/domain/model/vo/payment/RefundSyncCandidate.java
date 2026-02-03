package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentStatus;

/**
 * 退款单兜底同步候选 (用于任务扫描)
 *
 * @param refundId       退款单 ID
 * @param paypalRefundId PayPal Refund Order ID (可为空: 尚未创建网关订单则无需同步)
 * @param status         当前退款状态
 */
public record RefundSyncCandidate(@NotNull Long refundId,
                                  @Nullable String paypalRefundId,
                                  @NotNull PaymentStatus status) {
}
