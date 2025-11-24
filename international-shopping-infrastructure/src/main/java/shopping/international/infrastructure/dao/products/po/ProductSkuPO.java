package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: product_sku
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_sku")
public class ProductSkuPO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("sku_code")
    private String skuCode;
    @TableField("stock")
    private Integer stock;
    @TableField("weight")
    private BigDecimal weight;
    @TableField("status")
    private String status;
    @TableField("is_default")
    private Integer isDefault;
    @TableField("barcode")
    private String barcode;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
