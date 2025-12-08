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
import java.util.List;

/**
 * 商品SPU持久化对象, 对应表 product
 * <p>存储商品标准产品单元的基础信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product")
public class ProductPO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品标题
     */
    @TableField("title")
    private String title;

    /**
     * 商品副标题
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 商品描述
     */
    @TableField("description")
    private String description;

    /**
     * 商品别名, 用于 SEO 或路由
     */
    @TableField("slug")
    private String slug;

    /**
     * 分类ID, 指向 product_category.id
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 品牌名称
     */
    @TableField("brand")
    private String brand;

    /**
     * 主图 URL
     */
    @TableField("cover_image_url")
    private String coverImageUrl;

    /**
     * 总库存 (聚合所有 SKU)
     */
    @TableField("stock_total")
    private Integer stockTotal;

    /**
     * 总销量 (聚合所有 SKU)
     */
    @TableField("sale_count")
    private Integer saleCount;

    /**
     * 规格类型, SINGLE 或 VARIANT
     */
    @TableField("sku_type")
    private String skuType;

    /**
     * 商品状态
     */
    @TableField("status")
    private String status;

    /**
     * 默认展示的 SKU ID, 指向 product_sku.id
     */
    @TableField("default_sku_id")
    private Long defaultSkuId;

    /**
     * 标签 JSON 字段
     */
    @TableField("tags")
    private String tags;

    /**
     * 商品图片列表
     */
    private List<ProductImagePO> gallery;

    /**
     * 商品规格列表
     */
    private List<ProductSpecPO> specs;

    /**
     * 商品多语言列表
     */
    private List<ProductI18nPO> i18nList;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
