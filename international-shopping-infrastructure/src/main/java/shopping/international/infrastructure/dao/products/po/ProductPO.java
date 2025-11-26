package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product")
public class ProductPO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 商品标题
     */
    @TableField("title")
    private String title;
    /**
     * 副标题
     */
    @TableField("subtitle")
    private String subtitle;
    /**
     * 商品描述
     */
    @TableField("description")
    private String description;
    /**
     * 商品别名(SEO/路由)
     */
    @TableField("slug")
    private String slug;
    /**
     * 所属分类ID, 指向 product_category.id
     */
    @TableField("category_id")
    private Long categoryId;
    /**
     * 品牌
     */
    @TableField("brand")
    private String brand;
    /**
     * 主图URL
     */
    @TableField("cover_image_url")
    private String coverImageUrl;
    /**
     * 总库存(聚合)
     */
    @TableField("stock_total")
    private Integer stockTotal;
    /**
     * 销量(聚合)
     */
    @TableField("sale_count")
    private Integer saleCount;
    /**
     * 规格类型(单/多规格)
     */
    @TableField("sku_type")
    private String skuType;
    /**
     * 商品状态
     */
    @TableField("status")
    private String status;
    /**
     * 默认展示SKU, 指向 product_sku.id
     */
    @TableField("default_sku_id")
    private Long defaultSkuId;
    /**
     * 标签(JSON)
     */
    @TableField("tags")
    private String tags;
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
