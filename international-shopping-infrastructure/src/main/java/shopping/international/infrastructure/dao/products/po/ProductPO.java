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
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("title")
    private String title;
    @TableField("subtitle")
    private String subtitle;
    @TableField("description")
    private String description;
    @TableField("slug")
    private String slug;
    @TableField("category_id")
    private Long categoryId;
    @TableField("brand")
    private String brand;
    @TableField("cover_image_url")
    private String coverImageUrl;
    @TableField("stock_total")
    private Integer stockTotal;
    @TableField("sale_count")
    private Integer saleCount;
    @TableField("sku_type")
    private String skuType;
    @TableField("status")
    private String status;
    @TableField("default_sku_id")
    private Long defaultSkuId;
    @TableField("tags")
    private String tags;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
