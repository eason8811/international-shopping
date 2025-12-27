package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SKU-规格值映射持久化对象, 对应表 product_sku_spec
 * <p>关联 SKU 与所选规格值</p>
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
    @TableId(value = "sku_id", type = IdType.INPUT)
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
     * 规格编码 (冗余读取)
     */
    @TableField(exist = false)
    private String specCode;

    /**
     * 规格名称 (冗余读取)
     */
    @TableField(exist = false)
    private String specName;

    /**
     * 规格值编码 (冗余读取)
     */
    @TableField(exist = false)
    private String valueCode;

    /**
     * 规格值名称 (冗余读取)
     */
    @TableField(exist = false)
    private String valueName;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
