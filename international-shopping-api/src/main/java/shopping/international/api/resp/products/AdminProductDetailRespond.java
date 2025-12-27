package shopping.international.api.resp.products;

import lombok.*;
import lombok.experimental.SuperBuilder;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.util.List;

/**
 * 管理端商品详情响应 AdminProductDetailRespond
 *
 * <p>对应 ProductDetail schema, 包含 i18n_list、多币种价格与完整的规格信息</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminProductDetailRespond extends AbstractProductDetailRespond {
    /**
     * 规格列表
     */
    private List<AdminSpecRespond> specs;
    /**
     * 商品多语言列表
     */
    private List<ProductI18nRespond> i18nList;

    /**
     * 构造函数, 用于初始化管理端商品详情响应对象
     *
     * @param id            商品 ID
     * @param slug          商品标识符, 通常为 URL 友好的字符串
     * @param title         商品标题
     * @param subtitle      商品副标题
     * @param description   商品描述
     * @param categoryId    商品分类 ID
     * @param categorySlug  商品分类标识符
     * @param brand         品牌名称
     * @param coverImageUrl 封面图片 URL
     * @param stockTotal    总库存量
     * @param saleCount     销售数量
     * @param skuType       {@link SkuType} 商品规格类型
     * @param status        {@link ProductStatus} 商品状态
     * @param tags          标签列表
     * @param defaultSkuId  默认 SKU ID
     * @param gallery       商品图片列表
     * @param specs         商品规格信息列表
     * @param skus          商品 SKU 列表
     * @param i18nList      商品多语言信息列表
     */
    private AdminProductDetailRespond(Long id, String slug, String title, String subtitle, String description, Long categoryId, String categorySlug, String brand, String coverImageUrl, Integer stockTotal, Integer saleCount, SkuType skuType, ProductStatus status, List<String> tags, Long defaultSkuId, List<ProductImageRespond> gallery, List<AdminSpecRespond> specs, List<ProductSkuRespond> skus, List<ProductI18nRespond> i18nList) {
        super(id, slug, title, subtitle, description, categoryId, categorySlug, brand, coverImageUrl, stockTotal, saleCount, skuType, status, tags, defaultSkuId, gallery, skus);
        this.specs = specs;
        this.i18nList = i18nList;
    }

    /**
     * 商品多语言响应 ProductI18nRespond
     */
    @Data
    @Builder
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
    }
}
