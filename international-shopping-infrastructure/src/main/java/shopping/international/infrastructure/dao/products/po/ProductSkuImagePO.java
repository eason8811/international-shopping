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
 * SKU 图片持久化对象, 对应表 product_sku_image
 * <p>保存 SKU 专属的图片信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_sku_image")
public class ProductSkuImagePO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * SKU ID, 指向 product_sku.id
     */
    @Column("sku_id")
    private Long skuId;

    /**
     * 图片URL
     */
    @Column("url")
    private String url;

    /**
     * 是否为该 SKU 的主图
     */
    @Column("is_main")
    private Boolean isMain;

    /**
     * 排序, 值越小越靠前
     */
    @Column("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
}
