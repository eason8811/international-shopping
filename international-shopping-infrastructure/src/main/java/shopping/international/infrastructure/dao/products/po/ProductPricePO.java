package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: product_price
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_price")
public class ProductPricePO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SKU ID, 指向 product_sku.id
     */
    @TableField("sku_id")
    private Long skuId;
    /**
     * 币种, 指向 currency.code
     */
    @TableField("currency")
    private String currency;
    /**
     * 标价
     */
    @TableField("list_price")
    private BigDecimal listPrice;
    /**
     * 促销价 (可空)
     */
    @TableField("sale_price")
    private BigDecimal salePrice;
    /**
     * 是否可售用价
     */
    @TableField("is_active")
    private Integer isActive;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
