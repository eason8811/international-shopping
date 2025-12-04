package shopping.international.api.resp.products;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductDetail;
import shopping.international.domain.model.vo.products.ProductSpecValue;

import java.util.List;

/**
 * 用户侧商品详情响应 PublicProductDetailRespond
 *
 * <p>字段已经根据 locale 做过本地化替换, 不返回 i18n 信息</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicProductDetailRespond extends AbstractProductDetailRespond {
    /**
     * 构造一个公共商品详情响应对象, 用于展示给用户侧的商品详细信息
     *
     * @param id            商品 ID
     * @param slug          商品别名, 通常用于 URL 中
     * @param title         商品标题
     * @param subtitle      商品副标题
     * @param description   商品描述
     * @param categoryId    商品所属分类 ID
     * @param categorySlug  商品所属分类的别名
     * @param brand         品牌名称
     * @param coverImageUrl 封面图片 URL
     * @param stockTotal    总库存量
     * @param saleCount     销售数量
     * @param skuType       {@link SkuType} 商品规格类型, 单或多规格
     * @param status        {@link ProductStatus} 商品状态, 如草稿、上架等
     * @param tags          标签列表, 用于标识商品特性
     * @param defaultSkuId  默认 SKU ID
     * @param gallery       商品图库, 包含多个 {@link ProductImageRespond} 对象
     * @param specs         规格列表, 每个元素都是 {@code AbstractSpecRespond} 的子类实例
     * @param skus          SKU 列表, 包含多个 {@link ProductSkuRespond} 对象
     */
    private PublicProductDetailRespond(Long id, String slug, String title, String subtitle, String description, Long categoryId, String categorySlug, String brand, String coverImageUrl, Integer stockTotal, Integer saleCount, SkuType skuType, ProductStatus status, List<String> tags, Long defaultSkuId, List<ProductImageRespond> gallery, List<? extends AbstractSpecRespond> specs, List<ProductSkuRespond> skus) {
        super(id, slug, title, subtitle, description, categoryId, categorySlug, brand, coverImageUrl, stockTotal, saleCount, skuType, status, tags, defaultSkuId, gallery, specs, skus);
    }

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
    @EqualsAndHashCode(callSuper = true)
    public static class PublicSpecRespond extends AbstractSpecRespond {
        /**
         * 构造一个 PublicSpecRespond 实例, 用于表示用户侧规格响应
         *
         * @param specId     规格 ID
         * @param specCode   规格代码
         * @param specName   规格名称
         * @param specType   规格类型, 可以是 {@code COLOR}, {@code SIZE}, {@code CAPACITY}, {@code MATERIAL} 或 {@code OTHER}
         * @param isRequired 是否为必填规格
         * @param values     规格值列表, 其中每个元素都是 {@link AbstractSpecValueRespond} 的子类实例
         */
        private PublicSpecRespond(Long specId, String specCode, String specName, SpecType specType, Boolean isRequired, List<? extends AbstractSpecValueRespond> values) {
            super(specId, specCode, specName, specType, isRequired, values);
        }

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
    @EqualsAndHashCode(callSuper = true)
    public static class PublicSpecValueRespond extends AbstractSpecValueRespond {
        /**
         * 构造一个 PublicSpecValueRespond 实例, 用于表示用户侧规格值响应
         *
         * @param valueId    规格值 ID
         * @param valueCode  规格值代码
         * @param valueName  规格值名称
         * @param attributes 规格值属性
         */
        private PublicSpecValueRespond(Long valueId, String valueCode, String valueName, Object attributes) {
            super(valueId, valueCode, valueName, attributes);
        }

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
}
