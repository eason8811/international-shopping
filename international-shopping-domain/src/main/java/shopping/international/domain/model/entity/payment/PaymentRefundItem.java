package shopping.international.domain.model.entity.payment;

import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 退款明细实体 (对应表 payment_refund_item)
 *
 * <p>用于记录退款单按订单明细拆分的可选明细信息</p>
 *
 * @param id          主键 ID
 * @param refundId    退款单 ID (payment_refund.id)
 * @param orderItemId 订单明细 ID (order_item.id)
 * @param quantity    本次退款数量 (件)
 * @param amount      本次退款金额 (最小货币单位)
 * @param reason      该明细退款原因备注 (可选)
 * @param createdAt   创建时间
 */
public record PaymentRefundItem(
        Long id,
        Long refundId,
        Long orderItemId,
        int quantity,
        long amount,
        @Nullable String reason,
        @Nullable LocalDateTime createdAt) implements Verifiable {

    /**
     * 校验退款明细不变式
     */
    @Override
    public void validate() {
        requireNotNull(id, "refundItem.id 不能为空");
        requireNotNull(refundId, "refundItem.refundId 不能为空");
        requireNotNull(orderItemId, "refundItem.orderItemId 不能为空");
        require(quantity > 0, "refundItem.quantity 必须大于 0");
        require(amount > 0, "refundItem.amount 必须大于 0");
    }
}

