package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentStatus;

/**
 * 支付结果视图 (用于 Controller 对外响应)
 *
 * @param paymentId  支付单 ID
 * @param orderNo    订单号 (可空)
 * @param status     支付单状态
 * @param externalId 外部单号 (可空)
 * @param message    说明 (可空)
 */
public record PaymentResultView(@NotNull Long paymentId,
                                @Nullable String orderNo,
                                @NotNull PaymentStatus status,
                                @Nullable String externalId,
                                @Nullable String message) {
}
