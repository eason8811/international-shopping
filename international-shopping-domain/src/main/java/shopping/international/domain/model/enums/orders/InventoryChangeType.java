package shopping.international.domain.model.enums.orders;

/**
 * 库存变更类型
 * <ul>
 *     <li>{@code RESERVE} - 预占库存</li>
 *     <li>{@code DEDUCT} - 扣减库存</li>
 *     <li>{@code RELEASE} - 释放库存</li>
 *     <li>{@code RESTOCK} - 回补库存</li>
 * </ul>
 */
public enum InventoryChangeType {
    RESERVE,
    DEDUCT,
    RELEASE,
    RESTOCK
}
