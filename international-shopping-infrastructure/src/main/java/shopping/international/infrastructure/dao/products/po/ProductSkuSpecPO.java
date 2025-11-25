package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_sku_spec
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_sku_spec")
public class ProductSkuSpecPO {
    /**
     * SKU ID, 指向 product_sku.id
     */
    @TableId("sku_id")
    private Long skuId;
    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @TableField("spec_id")
    private Long specId;
    /**
     * 规格值ID, 指向 product_spec_value.id
     */
    @TableField("value_id")
    private Long valueId;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
