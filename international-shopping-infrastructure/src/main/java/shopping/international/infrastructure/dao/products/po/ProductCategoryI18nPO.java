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
 * 商品分类多语言持久化对象, 对应表 product_category_i18n
 * <p>存储分类名称、slug 与品牌文案的本地化内容</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_category_i18n")
public class ProductCategoryI18nPO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * 关联的分类ID, 指向 product_category.id
     */
    @Column("category_id")
    private Long categoryId;

    /**
     * 语言代码, 如 en_US
     */
    @Column("locale")
    private String locale;

    /**
     * 分类名称 (本地化)
     */
    @Column("name")
    private String name;

    /**
     * 分类 slug (本地化, 用于 SEO/路由)
     */
    @Column("slug")
    private String slug;

    /**
     * 分类品牌文案 (本地化)
     */
    @Column("brand")
    private String brand;

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
