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
     * 图片URL
     */
    @TableField("url")
    private String url;
    /**
     * 是否主图
     */
    @TableField("is_main")
    private Integer isMain;
    /**
     * 排序(小在前)
     */
    @TableField("sort_order")
    private Integer sortOrder;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
