package shopping.international.infrastructure.dao.shipping.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象, shipment_item 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("shipment_item")
public class ShipmentItemPO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 物流单主键
     */
    @TableField("shipment_id")
    private Long shipmentId;
    /**
     * 订单主键
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 订单明细主键
     */
    @TableField("order_item_id")
    private Long orderItemId;
    /**
     * 商品主键
     */
    @TableField("product_id")
    private Long productId;
    /**
     * SKU 主键
     */
    @TableField("sku_id")
    private Long skuId;
    /**
     * 数量
     */
    @TableField("quantity")
    private Integer quantity;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
