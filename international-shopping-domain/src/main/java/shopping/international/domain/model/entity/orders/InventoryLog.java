package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.InventoryChangeType;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 库存变动日志实体 (对应表 inventory_log)
 *
 * <p>用于审计与问题排查, 变动数量始终为正数, 变动方向由 changeType 表达。</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class InventoryLog implements Verifiable {
    /**
     * 主键ID (可为 {@code null}, 表示尚未持久化)
     */
    private Long id;
    /**
     * SKU ID, 指向 {@code product_sku.id}
     */
    private Long skuId;
    /**
     * 订单ID, 指向 {@code orders.id}
     */
    private Long orderId;
    /**
     * 变动类型 (预占/扣减/释放/回补)
     */
    private InventoryChangeType changeType;
    /**
     * 变动数量 (正数)
     */
    private int quantity;
    /**
     * 原因备注 (可空, 最长 255)
     */
    @Nullable
    private String reason;
    /**
     * 创建时间 (写入时间)
     */
    private LocalDateTime createdAt;

    /**
     * 创建一条新的库存变动日志 (用于追加写入)
     *
     * @param skuId      SKU ID
     * @param orderId    订单ID
     * @param changeType 变动类型
     * @param quantity   变动数量 (必须大于 0)
     * @param reason     原因备注, 可空
     * @return 新建的 {@link InventoryLog} 实体, {@code id} 为空表示未持久化
     */
    public static InventoryLog create(Long skuId, Long orderId, InventoryChangeType changeType, int quantity, @Nullable String reason) {
        return new InventoryLog(null, skuId, orderId, changeType, quantity,
                reason == null ? null : reason.strip(), LocalDateTime.now());
    }

    /**
     * 校验当前日志实体字段是否满足基本不变式
     *
     * <ul>
     *     <li>{@code skuId/orderId/changeType/createdAt} 必填</li>
     *     <li>{@code quantity} 必须大于 0</li>
     *     <li>{@code reason} 若不为空, 长度不得超过 255</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(skuId, "SKU ID 不能为空");
        requireNotNull(orderId, "订单 ID 不能为空");
        requireNotNull(changeType, "库存变动类型不能为空");
        require(quantity > 0, "库存变动数量必须大于 0");
        if (reason != null)
            require(reason.length() <= 255, "原因备注最长 255 个字符");
        requireNotNull(createdAt, "创建时间不能为空");
    }
}
