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
 * 商品规格类别持久化对象, 对应表 product_spec
 * <p>定义商品下的规格类别及其属性</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_spec")
public class ProductSpecPO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * 商品ID, 指向 product.id
     */
    @Column("product_id")
    private Long productId;

    /**
     * 规格编码, 如 color、capacity
     */
    @Column("spec_code")
    private String specCode;

    /**
     * 规格名称, 如 颜色、容量
     */
    @Column("spec_name")
    private String specName;

    /**
     * 规格类型：COLOR、SIZE、CAPACITY、MATERIAL 或 OTHER
     */
    @Column("spec_type")
    private String specType;

    /**
     * 是否必选, 决定每个 SKU 是否必须有该规格值
     */
    @Column("is_required")
    private Boolean isRequired;

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
