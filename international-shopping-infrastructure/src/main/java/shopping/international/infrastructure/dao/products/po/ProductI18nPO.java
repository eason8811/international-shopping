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
 * 商品多语言持久化对象, 对应表 product_i18n
 * <p>记录商品标题、副标题、描述与 slug 的本地化内容</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_i18n")
public class ProductI18nPO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 语言代码, 如 en_US
     */
    @TableField("locale")
    private String locale;

    /**
     * 标题 (本地化)
     */
    @TableField("title")
    private String title;

    /**
     * 副标题 (本地化)
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 描述 (本地化)
     */
    @TableField("description")
    private String description;

    /**
     * 商品 slug (本地化, 用于 SEO/路由)
     */
    @TableField("slug")
    private String slug;

    /**
     * 标签 JSON (本地化)
     */
    @TableField("tags")
    private String tags;

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
