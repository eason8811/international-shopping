package shopping.international.infrastructure.dao.products.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品SKU持久化对象, 对应表 product_sku
 * <p>记录商品销售单元的规格、库存与状态</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_sku")
public class ProductSkuPO {

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
     * SKU 编码或外部条码
     */
    @Column("sku_code")
    private String skuCode;

    /**
     * 可售库存
     */
    @Column("stock")
    private Integer stock;

    /**
     * 重量（公斤）
     */
    @Column("weight")
    private BigDecimal weight;

    /**
     * 启用状态, ENABLED 或 DISABLED
     */
    @Column("status")
    private String status;

    /**
     * 是否为该商品的默认展示 SKU
     */
    @Column("is_default")
    private Boolean isDefault;

    /**
     * 条码, 可为空
     */
    @Column("barcode")
    private String barcode;

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
