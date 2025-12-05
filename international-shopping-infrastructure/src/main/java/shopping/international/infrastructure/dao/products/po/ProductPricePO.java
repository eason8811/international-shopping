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
 * SKU 多币种价格持久化对象, 对应表 product_price
 * <p>记录 SKU 在不同结算币种下的价格</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_price")
public class ProductPricePO {

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
     * 币种代码 (ISO 4217)
     */
    @Column("currency")
    private String currency;

    /**
     * 标价
     */
    @Column("list_price")
    private BigDecimal listPrice;

    /**
     * 促销价, 可为空
     */
    @Column("sale_price")
    private BigDecimal salePrice;

    /**
     * 是否启用该价格
     */
    @Column("is_active")
    private Boolean isActive;

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
