package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductDetail;
import shopping.international.domain.model.vo.products.ProductSkuSpec;
import shopping.international.domain.model.vo.products.ProductSpecValue;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户侧商品详情响应 PublicProductDetailRespond
 *
 * <p>字段已经根据 locale 做过本地化替换, 不返回 i18n 信息</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicProductDetailRespond {
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
    private List<PublicSpecRespond> specs;
    /**
     * SKU 列表
     */
    private List<ProductSkuRespond> skus;

    /**
     * 构建用户侧商品详情响应
     *
     * @param detail 商品详情
     * @return 详情响应
     */
    public static PublicProductDetailRespond from(ProductDetail detail) {
        List<ProductImageRespond> gallery = detail.gallery() == null
                ? List.of()
                : detail.gallery().stream().map(ProductImageRespond::from).toList();
        List<PublicSpecRespond> publicSpecResponds = detail.specs() == null
                ? List.of()
                : detail.specs().stream().map(PublicSpecRespond::from).toList();
        List<ProductSkuRespond> productSkuResponds = detail.skus() == null
                ? List.of()
                : detail.skus().stream().map(ProductSkuRespond::from).toList();
        return new PublicProductDetailRespond(
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
                publicSpecResponds,
                productSkuResponds
        );
    }

    /**
     * 用户侧规格响应 PublicSpecRespond
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicSpecRespond {
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
        private List<PublicSpecValueRespond> values;

        /**
         * 从规格实体构建响应
         *
         * @param spec 规格实体
         * @return 规格响应
         */
        public static PublicSpecRespond from(ProductSpec spec) {
            String displayName = spec.getI18nName() == null ? spec.getSpecName() : spec.getI18nName();
            List<PublicSpecValueRespond> valueResponds = spec.getValues() == null ? List.of()
                    : spec.getValues().stream().map(PublicSpecValueRespond::from).toList();
            return new PublicSpecRespond(
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
    public static class PublicSpecValueRespond {
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
        public static PublicSpecValueRespond from(ProductSpecValue value) {
            String displayName = value.getI18nName() == null ? value.getValueName() : value.getI18nName();
            return new PublicSpecValueRespond(
                    value.getId(),
                    value.getValueCode(),
                    displayName,
                    value.getAttributes()
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
