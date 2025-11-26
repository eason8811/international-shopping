package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_i18n
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_i18n")
public class ProductI18nPO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SPU ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;
    /**
     * 语言代码, 指向 locale.code
     */
    @TableField("locale")
    private String locale;
    /**
     * 标题(本地化)
     */
    @TableField("title")
    private String title;
    /**
     * 副标题(本地化)
     */
    @TableField("subtitle")
    private String subtitle;
    /**
     * 描述(本地化)
     */
    @TableField("description")
    private String description;
    /**
     * 商品slug(本地化, 用于多语言路由/SEO)
     */
    @TableField("slug")
    private String slug;
    /**
     * 标签(本地化, JSON)
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
