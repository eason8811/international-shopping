package shopping.international.domain.model.enums.orders;

/**
 * 订单统计聚合维度
 * <ul>
 *     <li>{@code SPU} - 按 SPU 聚合</li>
 *     <li>{@code SKU} - 按 SKU 聚合</li>
 *     <li>{@code USER} - 按用户聚合</li>
 *     <li>{@code DISCOUNT_CODE} - 按折扣码聚合</li>
 *     <li>{@code DISCOUNT_POLICY} - 按折扣策略聚合</li>
 * </ul>
 */
public enum OrderStatsDimension {
    SPU,
    SKU,
    USER,
    DISCOUNT_CODE,
    DISCOUNT_POLICY
}
