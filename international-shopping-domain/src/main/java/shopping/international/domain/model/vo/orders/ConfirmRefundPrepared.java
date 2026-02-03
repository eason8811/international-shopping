package shopping.international.domain.model.vo.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 两段式确认退款退款准备结果
 *
 * @param orderId             订单 ID
 * @param orderNo             订单号
 * @param paymentOrderId      支付单 ID
 * @param paypalOrderId       外部支付单 ID (支付网关 ID)
 * @param captureId           capture ID
 * @param refundNo            退款单号
 * @param refundId            退款单 ID
 * @param clientRefundNo      客户端退款单号 (幂等键)
 * @param refundAmountMinor   退款金额 Minor 模式
 * @param currency            币种
 * @param itemsAmountMinor    商品总金额 Minor 模式
 * @param shippingAmountMinor 运费金额 Minor 模式
 * @param fullRefund          是否全额退款
 * @param shouldCallGateway   是否应该调用支付网关
 */
public record ConfirmRefundPrepared(@NotNull Long orderId,
                                    @NotNull String orderNo,
                                    @NotNull Long paymentOrderId,
                                    @NotNull String paypalOrderId,
                                    @Nullable String captureId,
                                    @NotNull String refundNo,
                                    @NotNull Long refundId,
                                    @NotNull String clientRefundNo,
                                    long refundAmountMinor,
                                    @NotNull String currency,
                                    @Nullable Long itemsAmountMinor,
                                    @Nullable Long shippingAmountMinor,
                                    boolean fullRefund,
                                    boolean shouldCallGateway) implements Verifiable {

    /**
     * 验证当前对象中所有必需字段是否已正确设置
     */
    @Override
    public void validate() {
        requireNotNull(orderId, "orderId 不能为空");
        requireNotBlank(orderNo, "orderNo 不能为空");
        requireNotNull(paymentOrderId, "paymentOrderId 不能为空");
        requireNotBlank(paypalOrderId, "paypalOrderId 不能为空");
        requireNotBlank(refundNo, "refundNo 不能为空");
        requireNotNull(refundId, "refundId 不能为空");
        requireNotBlank(clientRefundNo, "clientRefundNo 不能为空");
        requireNotBlank(currency, "currency 不能为空");
    }
}

