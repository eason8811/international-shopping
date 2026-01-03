package shopping.international.infrastructure.dao.products.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
     * 价格来源: MANUAL / FX_AUTO
     */
    @TableField("price_source")
    private String priceSource;

    /**
     * 派生基准币种 (通常 USD), price_source=FX_AUTO 时有效
     */
    @TableField("derived_from")
    private String derivedFrom;

    /**
     * 派生使用的汇率(1 derived_from = fx_rate currency)
     */
    @TableField("fx_rate")
    private BigDecimal fxRate;

    /**
     * 派生使用的汇率时间点
     */
    @TableField("fx_as_of")
    private LocalDateTime fxAsOf;

    /**
     * 派生使用的数据源
     */
    @TableField("fx_provider")
    private String fxProvider;

    /**
     * 价格计算时间
     */
    @TableField("computed_at")
    private LocalDateTime computedAt;

    /**
     * 算法版本
     */
    @TableField("algo_ver")
    private Integer algoVer;

    /**
     * 加价/手续费 (bps)
     */
    @TableField("markup_bps")
    private Integer markupBps;

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
