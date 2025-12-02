package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductDetail;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductSkuSpec;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端商品详情响应 AdminProductDetailRespond
 *
 * <p>对应 ProductDetail schema, 包含 i18n_list、多币种价格与完整的规格信息</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductDetailRespond {
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
     * 封面图地址
     */
    private String coverImageUrl;
    /**
     * 聚合库存
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
     * 商品图片
     */
    private List<ProductImageRespond> gallery;
    /**
     * 规格列表
     */
    private List<AdminSpecRespond> specs;
    /**
     * SKU 列表
     */
    private List<ProductSkuRespond> skus;
    /**
     * 商品多语言列表
     */
    private List<ProductI18nRespond> i18nList;

    /**
     * 构建管理端商品详情响应
     *
     * @param detail 领域层商品详情
     * @return 详情响应
     */
    public static AdminProductDetailRespond from(ProductDetail detail) {
        List<ProductImageRespond> gallery = detail.gallery() == null ? List.of()
                : detail.gallery().stream().map(ProductImageRespond::from).toList();
        List<AdminSpecRespond> adminSpecResponds = detail.specs() == null ? List.of()
                : detail.specs().stream().map(AdminSpecRespond::from).toList();
        List<ProductSkuRespond> productSkuResponds = detail.skus() == null ? List.of()
                : detail.skus().stream().map(ProductSkuRespond::from).toList();
        List<ProductI18nRespond> i18nResponds = detail.i18nList() == null ? List.of()
                : detail.i18nList().stream().map(ProductI18nRespond::from).toList();
        return new AdminProductDetailRespond(
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
                adminSpecResponds,
                productSkuResponds,
                i18nResponds
        );
    }

    /**
     * SKU 价格响应 ProductPriceRespond
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
     * SKU 规格绑定响应 ProductSkuSpecRespond
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

    /**
     * 商品多语言响应 ProductI18nRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductI18nRespond {
        /**
         * 语言代码
         */
        private String locale;
        /**
         * 本地化标题
         */
        private String title;
        /**
         * 本地化副标题
         */
        private String subtitle;
        /**
         * 本地化描述
         */
        private String description;
        /**
         * 本地化 slug
         */
        private String slug;
        /**
         * 本地化标签
         */
        private List<String> tags;

        /**
         * 从商品多语言实体构建响应
         *
         * @param vo 商品多语言实体
         * @return 商品多语言响应
         */
        public static ProductI18nRespond from(ProductI18n vo) {
            return new ProductI18nRespond(vo.getLocale(), vo.getTitle(), vo.getSubtitle(), vo.getDescription(), vo.getSlug(), vo.getTags());
        }
    }
}
