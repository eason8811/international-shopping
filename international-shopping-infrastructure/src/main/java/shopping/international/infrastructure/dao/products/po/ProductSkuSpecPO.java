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
    @TableId("sku_id")
    private Long skuId;
    @TableField("spec_id")
    private Long specId;
    @TableField("value_id")
    private Long valueId;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
