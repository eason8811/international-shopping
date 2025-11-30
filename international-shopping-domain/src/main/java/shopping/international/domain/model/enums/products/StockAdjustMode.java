package shopping.international.domain.model.enums.products;

/**
 * 库存调整模式
 *
 * <p>用于区分库存设置与增减行为, 便于在领域层统一处理库存变化</p>
 *
 * <ul>
 *     <li>{@code SET}: 直接设置库存为指定值</li>
 *     <li>{@code INCREASE}: 在当前库存基础上增加</li>
 *     <li>{@code DECREASE}: 在当前库存基础上减少</li>
 * </ul>
 */
public enum StockAdjustMode {
    SET, INCREASE, DECREASE
}
