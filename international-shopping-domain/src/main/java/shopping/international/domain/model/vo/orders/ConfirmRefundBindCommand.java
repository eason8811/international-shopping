package shopping.international.domain.model.vo.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * <p>确认退款绑定命令, 用于在系统中确认特定订单的退款状态</p>
 *
 * @param orderId          订单 ID
 * @param orderNo          订单号
 * @param paymentOrderId   支付单 ID
 * @param refundId         退款单 ID
 * @param externalRefundId 外部退款单 ID
 * @param status           状态
 * @param requestPayload   请求体
 * @param responsePayload  响应体
 * @param note             备注
 * @param fullRefund       是否全额退款
 */
public record ConfirmRefundBindCommand(@NotNull Long orderId,
                                       @NotNull String orderNo,
                                       @NotNull Long paymentOrderId,
                                       @NotNull Long refundId,
                                       @Nullable String externalRefundId,
                                       @NotNull RefundStatus status,
                                       @Nullable String requestPayload,
                                       @Nullable String responsePayload,
                                       @Nullable String note,
                                       boolean fullRefund) implements Verifiable {

    /**
     * 验证当前命令实例中的关键字段是否满足非空要求
     */
    @Override
    public void validate() {
        requireNotNull(orderId, "orderId 不能为空");
        requireNotBlank(orderNo, "orderNo 不能为空");
        requireNotNull(paymentOrderId, "paymentOrderId 不能为空");
        requireNotNull(refundId, "refundId 不能为空");
        requireNotNull(status, "status 不能为空");
    }
}

