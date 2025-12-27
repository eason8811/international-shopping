package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.InventoryChangeType;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 库存日志筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogSearchCriteria implements Verifiable {
    /**
     * 变更类型过滤 (可空)
     */
    @Nullable
    private InventoryChangeType changeType;
    /**
     * SKU ID 过滤 (可空)
     */
    @Nullable
    private Long skuId;
    /**
     * 订单 ID 过滤 (可空)
     */
    @Nullable
    private Long orderId;
    /**
     * 数量下限 (可空, 含)
     */
    @Nullable
    private Integer quantityMin;
    /**
     * 数量上限 (可空, 含)
     */
    @Nullable
    private Integer quantityMax;
    /**
     * 时间起 (可空, 含)
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 时间止 (可空, 含)
     */
    @Nullable
    private LocalDateTime createdTo;

    /**
     * 校验筛选条件
     *
     * <ul>
     *     <li>若同时提供 {@code quantityMin/quantityMax}, 则保证区间不反转</li>
     *     <li>若同时提供 {@code createdFrom/createdTo}, 则保证区间不反转</li>
     * </ul>
     */
    @Override
    public void validate() {
        if (quantityMin != null && quantityMax != null)
            require(quantityMin <= quantityMax, "quantityMin 不能大于 quantityMax");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");
    }
}

