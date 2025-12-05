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
 * 商品图片持久化对象, 对应表 product_image
 * <p>保存商品 SPU 的图片信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_image")
public class ProductImagePO {

    /**
     * 主键ID, 自增
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;

    /**
     * 商品ID, 指向 product.id
     */
    @Column("product_id")
    private Long productId;

    /**
     * 图片URL
     */
    @Column("url")
    private String url;

    /**
     * 是否主图
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
