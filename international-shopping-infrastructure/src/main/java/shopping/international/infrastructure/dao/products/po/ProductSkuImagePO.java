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
 * SKU 图片持久化对象, 对应表 product_sku_image
 * <p>保存 SKU 专属的图片信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_sku_image")
public class ProductSkuImagePO {

    /**
     * 主键ID, 自增
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
     * 是否为该 SKU 的主图
     */
    @TableField("is_main")
    private Boolean isMain;

    /**
     * 排序, 值越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
