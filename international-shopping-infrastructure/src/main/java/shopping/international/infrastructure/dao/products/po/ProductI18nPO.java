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
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("locale")
    private String locale;
    @TableField("title")
    private String title;
    @TableField("subtitle")
    private String subtitle;
    @TableField("description")
    private String description;
    @TableField("slug")
    private String slug;
    @TableField("tags")
    private String tags;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
