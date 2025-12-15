package shopping.international.domain.model.enums.orders;

/**
 * 折扣应用范围
 * <ul>
 *     <li>{@code ORDER} - 订单级折扣, 作用于整单金额</li>
 *     <li>{@code ITEM} - 明细级折扣, 作用于订单明细</li>
 * </ul>
 */
public enum DiscountApplyScope {
    ORDER,
    ITEM
}
