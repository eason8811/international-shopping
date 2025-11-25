package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: product_sku_image
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_sku_image")
public class ProductSkuImagePO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * SKU ID, 指向 product_sku.id
     */
    @TableField("sku_id")
    private Long skuId;
    /**
     * 图片URL
     */
    @TableField("url")
    private String url;
    /**
     * 是否主图(该SKU范围内)
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
