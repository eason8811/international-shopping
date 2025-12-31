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
 * SKU 多币种价格持久化对象, 对应表 product_price
 * <p>记录 SKU 在不同结算币种下的价格</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_price")
public class ProductPricePO {

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
     * 币种代码 (ISO 4217)
     */
    @TableField("currency")
    private String currency;

    /**
     * 标价（最小货币单位）
     */
    @TableField("list_price")
    private Long listPrice;

    /**
     * 促销价（最小货币单位）, 可为空
     */
    @TableField("sale_price")
    private Long salePrice;

    /**
     * 是否启用该价格
     */
    @TableField("is_active")
    private Boolean isActive;

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
