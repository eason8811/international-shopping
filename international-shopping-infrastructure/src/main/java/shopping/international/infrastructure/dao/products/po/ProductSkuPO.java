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
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SPU ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;
    /**
     * SKU编码(外部/条码等)
     */
    @TableField("sku_code")
    private String skuCode;
    /**
     * 可售库存
     */
    @TableField("stock")
    private Integer stock;
    /**
     * 重量(kg)
     */
    @TableField("weight")
    private BigDecimal weight;
    /**
     * 状态
     */
    @TableField("status")
    private String status;
    /**
     * 是否默认展示SKU
     */
    @TableField("is_default")
    private Integer isDefault;
    /**
     * 条码(可空)
     */
    @TableField("barcode")
    private String barcode;
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
