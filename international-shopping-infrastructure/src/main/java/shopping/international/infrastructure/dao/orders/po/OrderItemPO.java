package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: order_item
 *
 * <p>订单明细表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("order_item")
public class OrderItemPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 商品 ID
     */
    @TableField("product_id")
    private Long productId;
    /**
     * SKU ID
     */
    @TableField("sku_id")
    private Long skuId;
    /**
     * 折扣码 ID
     */
    @TableField("discount_code_id")
    private Long discountCodeId;
    /**
     * 商品标题快照
     */
    @TableField("title")
    private String title;
    /**
     * SKU 属性快照 JSON
     */
    @TableField("sku_attrs")
    private String skuAttrs;
    /**
     * 封面图快照
     */
    @TableField("cover_image_url")
    private String coverImageUrl;
    /**
     * 单价快照
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;
    /**
     * 数量
     */
    @TableField("quantity")
    private Integer quantity;
    /**
     * 小计金额
     */
    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

