package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentStatus;

/**
 * 兜底同步候选 (用于任务扫描)
 *
 * @param paymentId     支付单 ID
 * @param paypalOrderId PayPal Order ID (可为空: 尚未创建网关订单则无需同步)
 * @param status        当前支付单状态
 */
public record SyncCandidate(@NotNull Long paymentId,
                            @Nullable String paypalOrderId,
                            @NotNull PaymentStatus status) {
}
