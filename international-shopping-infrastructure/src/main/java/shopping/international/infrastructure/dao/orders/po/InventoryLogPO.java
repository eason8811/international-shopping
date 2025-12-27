package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: inventory_log
 *
 * <p>库存变动日志表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("inventory_log")
public class InventoryLogPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SKU ID
     */
    @TableField("sku_id")
    private Long skuId;
    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 变更类型
     */
    @TableField("change_type")
    private String changeType;
    /**
     * 数量
     */
    @TableField("quantity")
    private Integer quantity;
    /**
     * 原因备注
     */
    @TableField("reason")
    private String reason;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

