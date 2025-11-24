package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_image")
public class ProductImagePO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("product_id")
    private Long productId;
    @TableField("url")
    private String url;
    @TableField("is_main")
    private Integer isMain;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
