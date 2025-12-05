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
 * 规格值持久化对象, 对应表 product_spec_value
 * <p>记录规格类别下的可选值及其属性</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_spec_value")
public class ProductSpecValuePO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * 商品ID, 指向 product.id (冗余便于校验与查询)
     */
    @Column("product_id")
    private Long productId;

    /**
     * 规格类别ID, 指向 product_spec.id
     */
    @Column("spec_id")
    private Long specId;

    /**
     * 规格值编码, 如 black, gray, 512gb
     */
    @Column("value_code")
    private String valueCode;

    /**
     * 规格值名称
     */
    @Column("value_name")
    private String valueName;

    /**
     * 附加属性 JSON, 如颜色 hex 或展示图等
     */
    @Column("attributes")
    private String attributes;

    /**
     * 排序, 值越小越靠前
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 启用状态, ENABLED 或 DISABLED
     */
    @Column("status")
    private String status;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
