package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户侧商品详情响应 ProductDetailPublicRespond
 *
 * <p>字段已经根据 locale 做过本地化替换, 不返回 i18n 信息</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailPublicRespond {
    /**
     * 商品 ID
     */
    private Long id;
    /**
     * 商品 slug
     */
    private String slug;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 商品副标题
     */
    private String subtitle;
    /**
     * 商品描述
     */
    private String description;
    /**
     * 分类 ID
     */
    private Long categoryId;
    /**
     * 分类 slug
     */
    private String categorySlug;
    /**
     * 品牌文案
     */
    private String brand;
    /**
     * 封面图
     */
    private String coverImageUrl;
    /**
     * 库存总量
     */
    private Integer stockTotal;
    /**
     * 销量
     */
    private Integer saleCount;
    /**
     * SKU 类型
     */
    private SkuType skuType;
    /**
     * 商品状态
     */
    private ProductStatus status;
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 默认 SKU ID
     */
    private Long defaultSkuId;
    /**
     * 商品图库
     */
    private List<ProductImageRespond> gallery;
    /**
     * 规格列表
     */
    private List<SpecRespond> specs;
    /**
     * SKU 列表
     */
    private List<SkuRespond> skus;

    /**
     * 构建用户侧商品详情响应
     *
     * @param detail 商品详情
     * @return 详情响应
     */
    public static ProductDetailPublicRespond from(ProductDetail detail) {
        List<ProductImageRespond> gallery = detail.gallery() == null ? List.of()
                : detail.gallery().stream().map(ProductImageRespond::from).toList();
        List<SpecRespond> specResponds = detail.specs() == null ? List.of()
                : detail.specs().stream().map(SpecRespond::from).toList();
        List<SkuRespond> skuResponds = detail.skus() == null ? List.of()
                : detail.skus().stream().map(SkuRespond::from).toList();
        return new ProductDetailPublicRespond(
                detail.id(),
                detail.slug(),
                detail.title(),
                detail.subtitle(),
                detail.description(),
                detail.categoryId(),
                detail.categorySlug(),
                detail.brand(),
                detail.coverImageUrl(),
                detail.stockTotal(),
                detail.saleCount(),
                detail.skuType(),
                detail.status(),
                detail.tags(),
                detail.defaultSkuId(),
                gallery,
                specResponds,
                skuResponds
        );
    }

    /**
     * 用户侧规格响应 SpecRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecRespond {
        /**
         * 规格 ID
         */
        private Long specId;
        /**
         * 规格编码
         */
        private String specCode;
        /**
         * 规格名称
         */
        private String specName;
        /**
         * 规格类型
         */
        private SpecType specType;
        /**
         * 是否必选
         */
        private Boolean isRequired;
        /**
         * 规格值列表
         */
        private List<SpecValueRespond> values;

        /**
         * 从规格实体构建响应
         *
         * @param spec 规格实体
         * @return 规格响应
         */
        public static SpecRespond from(ProductSpec spec) {
            String displayName = spec.getI18nName() == null ? spec.getSpecName() : spec.getI18nName();
            List<SpecValueRespond> valueResponds = spec.getValues() == null ? List.of()
                    : spec.getValues().stream().map(SpecValueRespond::from).toList();
            return new SpecRespond(
                    spec.getId(),
                    spec.getSpecCode(),
                    displayName,
                    spec.getSpecType(),
                    spec.isRequired(),
                    valueResponds
            );
        }
    }

    /**
     * 用户侧规格值响应 SpecValueRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecValueRespond {
        /**
         * 规格值 ID
         */
        private Long valueId;
        /**
         * 规格值编码
         */
        private String valueCode;
        /**
         * 规格值名称
         */
        private String valueName;
        /**
         * 规格值属性
         */
        private Object attributes;

        /**
         * 从规格值实体构建响应
         *
         * @param value 规格值实体
         * @return 规格值响应
         */
        public static SpecValueRespond from(ProductSpecValue value) {
            String displayName = value.getI18nName() == null ? value.getValueName() : value.getI18nName();
            return new SpecValueRespond(
                    value.getId(),
                    value.getValueCode(),
                    displayName,
                    value.getAttributes()
            );
        }
    }

    /**
     * 用户侧 SKU 响应 SkuRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuRespond {
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
        private List<ProductPriceRespond> price;
        /**
         * 规格绑定
         */
        private List<ProductSkuSpecRespond> specs;
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
        public static SkuRespond from(ProductSku sku) {
            List<ProductPriceRespond> priceList = sku.getPrices() == null ? List.of()
                    : sku.getPrices().stream()
                    .map(price -> new ProductPriceRespond(price.getCurrency(), price.getListPrice(), price.getSalePrice(), price.isActive()))
                    .toList();
            List<ProductSkuSpecRespond> specs = sku.getSpecs() == null ? List.of()
                    : sku.getSpecs().stream().map(ProductSkuSpecRespond::from).toList();
            List<ProductImageRespond> images = sku.getImages() == null ? List.of()
                    : sku.getImages().stream().map(ProductImageRespond::from).toList();
            return new SkuRespond(
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

    /**
     * 用户侧价格响应 ProductPriceRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPriceRespond {
        /**
         * 币种
         */
        private String currency;
        /**
         * 标价
         */
        private BigDecimal listPrice;
        /**
         * 促销价
         */
        private BigDecimal salePrice;
        /**
         * 是否启用
         */
        private Boolean isActive;
    }

    /**
     * 用户侧 SKU 规格绑定响应 ProductSkuSpecRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSkuSpecRespond {
        /**
         * 规格 ID
         */
        private Long specId;
        /**
         * 规格编码
         */
        private String specCode;
        /**
         * 规格名称
         */
        private String specName;
        /**
         * 规格值 ID
         */
        private Long valueId;
        /**
         * 规格值编码
         */
        private String valueCode;
        /**
         * 规格值名称
         */
        private String valueName;

        /**
         * 从规格绑定实体构建响应
         *
         * @param spec 规格绑定实体
         * @return 规格绑定响应
         */
        public static ProductSkuSpecRespond from(ProductSkuSpec spec) {
            return new ProductSkuSpecRespond(
                    spec.getSpecId(),
                    spec.getSpecCode(),
                    spec.getSpecName(),
                    spec.getValueId(),
                    spec.getValueCode(),
                    spec.getValueName()
            );
        }
    }
}
