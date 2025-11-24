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
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("sku_id")
    private Long skuId;
    @TableField("currency")
    private String currency;
    @TableField("list_price")
    private BigDecimal listPrice;
    @TableField("sale_price")
    private BigDecimal salePrice;
    @TableField("is_active")
    private Integer isActive;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
