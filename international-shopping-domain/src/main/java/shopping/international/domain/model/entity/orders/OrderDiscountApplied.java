package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.vo.orders.Money;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 折扣应用事实实体 (对应表 order_discount_applied)
 *
 * <p>该实体表达“真实抵扣”结果, 不是折扣规则本身。</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OrderDiscountApplied implements Verifiable {
    /**
     * 主键ID (可为 {@code null}, 表示尚未持久化)
     */
    private Long id;
    /**
     * 订单ID, 指向 {@code orders.id}
     */
    private Long orderId;
    /**
     * 订单明细ID (明细级折扣必填; 订单级折扣为 {@code null})
     */
    @Nullable
    private Long orderItemId;
    /**
     * 折扣码ID, 指向 {@code discount_code.id}
     */
    private Long discountCodeId;
    /**
     * 应用范围 (订单级/明细级)
     */
    private DiscountApplyScope appliedScope;
    /**
     * 本次实际抵扣金额 (订单币种)
     */
    private Money appliedAmount;
    /**
     * 创建时间 (写入时间)
     */
    private LocalDateTime createdAt;

    /**
     * 创建订单级折扣应用事实
     *
     * @param orderId        订单ID
     * @param discountCodeId 折扣码ID
     * @param appliedAmount  实际抵扣金额
     * @return 订单级 {@link OrderDiscountApplied}
     */
    public static OrderDiscountApplied orderLevel(Long orderId, Long discountCodeId, Money appliedAmount) {
        return new OrderDiscountApplied(null, orderId, null, discountCodeId, DiscountApplyScope.ORDER, appliedAmount, LocalDateTime.now());
    }

    /**
     * 创建明细级折扣应用事实
     *
     * @param orderId        订单ID
     * @param orderItemId    订单明细ID
     * @param discountCodeId 折扣码ID
     * @param appliedAmount  实际抵扣金额
     * @return 明细级 {@link OrderDiscountApplied}
     */
    public static OrderDiscountApplied itemLevel(Long orderId, Long orderItemId, Long discountCodeId, Money appliedAmount) {
        return new OrderDiscountApplied(null, orderId, orderItemId, discountCodeId, DiscountApplyScope.ITEM, appliedAmount, LocalDateTime.now());
    }

    /**
     * 校验当前事实实体字段是否满足基本不变式
     *
     * <ul>
     *     <li>{@code orderId/discountCodeId/appliedScope/appliedAmount/createdAt} 必填</li>
     *     <li>{@code appliedAmount.amount} 必须大于 0</li>
     *     <li>当 {@code appliedScope == ITEM} 时, {@code orderItemId} 必填</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(orderId, "订单 ID 不能为空");
        requireNotNull(discountCodeId, "折扣码 ID 不能为空");
        requireNotNull(appliedScope, "折扣应用范围不能为空");
        requireNotNull(appliedAmount, "抵扣金额不能为空");
        require(appliedAmount.getAmount().signum() > 0, "抵扣金额必须大于 0");
        if (appliedScope == DiscountApplyScope.ITEM)
            requireNotNull(orderItemId, "明细级折扣必须提供 orderItemId");
        requireNotNull(createdAt, "创建时间不能为空");
    }
}
