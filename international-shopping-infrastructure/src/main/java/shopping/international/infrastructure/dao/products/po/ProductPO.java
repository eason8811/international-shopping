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
 * 商品SPU持久化对象, 对应表 product
 * <p>存储商品标准产品单元的基础信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product")
public class ProductPO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * 商品标题
     */
    @Column("title")
    private String title;

    /**
     * 商品副标题
     */
    @Column("subtitle")
    private String subtitle;

    /**
     * 商品描述
     */
    @Column("description")
    private String description;

    /**
     * 商品别名, 用于 SEO 或路由
     */
    @Column("slug")
    private String slug;

    /**
     * 分类ID, 指向 product_category.id
     */
    @Column("category_id")
    private Long categoryId;

    /**
     * 品牌名称
     */
    @Column("brand")
    private String brand;

    /**
     * 主图 URL
     */
    @Column("cover_image_url")
    private String coverImageUrl;

    /**
     * 总库存 (聚合所有 SKU)
     */
    @Column("stock_total")
    private Integer stockTotal;

    /**
     * 总销量 (聚合所有 SKU)
     */
    @Column("sale_count")
    private Integer saleCount;

    /**
     * 规格类型, SINGLE 或 VARIANT
     */
    @Column("sku_type")
    private String skuType;

    /**
     * 商品状态
     */
    @Column("status")
    private String status;

    /**
     * 默认展示的 SKU ID, 指向 product_sku.id
     */
    @Column("default_sku_id")
    private Long defaultSkuId;

    /**
     * 标签 JSON 字段
     */
    @Column("tags")
    private String tags;

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
