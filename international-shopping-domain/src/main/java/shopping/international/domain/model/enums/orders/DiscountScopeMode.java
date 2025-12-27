package shopping.international.domain.model.enums.orders;

/**
 * 折扣码适用范围模式
 * <ul>
 *     <li>{@code ALL} - 不使用映射表, 所有商品可用</li>
 *     <li>{@code INCLUDE} - 仅映射表中的商品可用</li>
 *     <li>{@code EXCLUDE} - 除映射表中的商品外均可用</li>
 * </ul>
 */
public enum DiscountScopeMode {
    ALL,
    INCLUDE,
    EXCLUDE
}
