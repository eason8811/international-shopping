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

/**
 * 商品分类多语言持久化对象, 对应表 product_category_i18n
 * <p>存储分类名称、slug 与品牌文案的本地化内容</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_category_i18n")
public class ProductCategoryI18nPO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的分类ID, 指向 product_category.id
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 语言代码, 如 en_US
     */
    @TableField("locale")
    private String locale;

    /**
     * 分类名称 (本地化)
     */
    @TableField("name")
    private String name;

    /**
     * 分类 slug (本地化, 用于 SEO/路由)
     */
    @TableField("slug")
    private String slug;

    /**
     * 分类品牌文案 (本地化)
     */
    @TableField("brand")
    private String brand;

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
