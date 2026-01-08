package shopping.international.domain.model.enums.orders;

/**
 * 折扣策略币种金额配置来源
 * <ul>
 *     <li>{@code MANUAL:} 人工录入/固定配置</li>
 *     <li>{@code FX_AUTO:} 基于基准币种 + 汇率自动派生</li>
 * </ul>
 */
public enum DiscountPolicyAmountSource {
    MANUAL, FX_AUTO
}

