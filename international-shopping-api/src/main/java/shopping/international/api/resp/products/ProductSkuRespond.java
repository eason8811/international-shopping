package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.enums.products.SkuStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 公共 SKU 响应 ProductSkuRespond
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuRespond {
    /**
     * SKU ID
     */
    private Long id;
    /**
     * SKU 编码
     */
    private String skuCode;
    /**
     * 库存
     */
    private Integer stock;
    /**
     * 重量
     */
    private BigDecimal weight;
    /**
     * 状态
     */
    private SkuStatus status;
    /**
     * 是否默认
     */
    private Boolean isDefault;
    /**
     * 条码
     */
    private String barcode;
    /**
     * 价格列表
     */
    private List<PublicProductDetailRespond.ProductPriceRespond> price;
    /**
     * 规格绑定
     */
    private List<PublicProductDetailRespond.ProductSkuSpecRespond> specs;
    /**
     * 图片列表
     */
    private List<ProductImageRespond> images;

    /**
     * 从 SKU 实体构建响应
     *
     * @param sku SKU 实体
     * @return SKU 响应
     */
    public static ProductSkuRespond from(ProductSku sku) {
        List<PublicProductDetailRespond.ProductPriceRespond> priceList = sku.getPrices() == null ? List.of()
                : sku.getPrices().stream()
                .map(price -> new PublicProductDetailRespond.ProductPriceRespond(price.getCurrency(), price.getListPrice(), price.getSalePrice(), price.isActive()))
                .toList();
        List<PublicProductDetailRespond.ProductSkuSpecRespond> specs = sku.getSpecs() == null ? List.of()
                : sku.getSpecs().stream().map(PublicProductDetailRespond.ProductSkuSpecRespond::from).toList();
        List<ProductImageRespond> images = sku.getImages() == null ? List.of()
                : sku.getImages().stream().map(ProductImageRespond::from).toList();
        return new ProductSkuRespond(
                sku.getId(),
                sku.getSkuCode(),
                sku.getStock(),
                sku.getWeight(),
                sku.getStatus(),
                sku.isDefault(),
                sku.getBarcode(),
                priceList,
                specs,
                images
        );
    }
}
