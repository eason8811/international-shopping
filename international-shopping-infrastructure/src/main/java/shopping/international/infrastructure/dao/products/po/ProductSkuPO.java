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
import java.util.List;

/**
 * 商品SKU持久化对象, 对应表 product_sku
 * <p>记录商品销售单元的规格、库存与状态</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_sku")
public class ProductSkuPO {

    /**
     * 主键ID, 自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID, 指向 product.id
     */
    @TableField("product_id")
    private Long productId;

    /**
     * SKU 编码或外部条码
     */
    @TableField("sku_code")
    private String skuCode;

    /**
     * 可售库存
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 重量（公斤）
     */
    @TableField("weight")
    private BigDecimal weight;

    /**
     * 启用状态, ENABLED 或 DISABLED
     */
    @TableField("status")
    private String status;

    /**
     * 是否为该商品的默认展示 SKU
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 条码, 可为空
     */
    @TableField("barcode")
    private String barcode;

    /**
     * SKU 多币种价格列表
     */
    @TableField(exist = false)
    private List<ProductPricePO> prices;

    /**
     * SKU 与规格值关联信息列表
     */
    @TableField(exist = false)
    private List<ProductSkuSpecPO> specs;

    /**
     * SKU 图片列表
     */
    @TableField(exist = false)
    private List<ProductSkuImagePO> images;

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
