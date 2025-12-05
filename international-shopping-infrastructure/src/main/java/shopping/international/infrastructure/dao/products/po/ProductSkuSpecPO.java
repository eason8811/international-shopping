package shopping.international.infrastructure.dao.products.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
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
@Table("product_sku_spec")
public class ProductSkuSpecPO {

    /**
     * SKU ID, 指向 product_sku.id
     */
    @Id(keyType = KeyType.None)
    @Column("sku_id")
    private Long skuId;

    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @Id(keyType = KeyType.None)
    @Column("spec_id")
    private Long specId;

    /**
     * 规格值ID, 指向 product_spec_value.id
     */
    @Column("value_id")
    private Long valueId;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}
