package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_category_i18n
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_category_i18n")
public class ProductCategoryI18nPO {
    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 分类 ID
     */
    @TableField("category_id")
    private Long categoryId;
    /**
     * 语言
     */
    @TableField("locale")
    private String locale;
    /**
     * 本地化分类名称
     */
    @TableField("name")
    private String name;
    /**
     * 分类slug(本地化, 用于多语言路由/SEO)
     */
    @TableField("slug")
    private String slug;
    /**
     * 本地化分类品牌文案
     */
    @TableField("brand")
    private String brand;
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
