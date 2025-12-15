package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.InventoryChangeType;

import java.time.LocalDateTime;

/**
 * 库存变更日志响应 (InventoryLogRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogRespond {
    /**
     * 日志 ID
     */
    private Long id;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 变更类型
     */
    private InventoryChangeType changeType;
    /**
     * 数量
     */
    private Integer quantity;
    /**
     * 变更原因 (可为空)
     */
    private String reason;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

