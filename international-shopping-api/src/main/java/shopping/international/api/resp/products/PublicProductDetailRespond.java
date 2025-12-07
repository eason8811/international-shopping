package shopping.international.api.resp.products;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;

import java.util.List;

/**
 * 用户侧商品详情响应 PublicProductDetailRespond
 *
 * <p>字段已经根据 locale 做过本地化替换, 不返回 i18n 信息</p>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicProductDetailRespond extends AbstractProductDetailRespond {
    /**
     * 规格列表
     */
    private List<PublicSpecRespond> specs;

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
    private PublicProductDetailRespond(Long id, String slug, String title, String subtitle, String description, Long categoryId, String categorySlug, String brand, String coverImageUrl, Integer stockTotal, Integer saleCount, SkuType skuType, ProductStatus status, List<String> tags, Long defaultSkuId, List<ProductImageRespond> gallery, List<PublicSpecRespond> specs, List<ProductSkuRespond> skus) {
        super(id, slug, title, subtitle, description, categoryId, categorySlug, brand, coverImageUrl, stockTotal, saleCount, skuType, status, tags, defaultSkuId, gallery, skus);
        this.specs = specs;
    }

    /**
     * 用户侧规格响应 PublicSpecRespond
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class PublicSpecRespond extends AbstractSpecRespond {
        /**
         * 规格值列表
         */
        private List<PublicSpecValueRespond> values;

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
        private PublicSpecRespond(Long specId, String specCode, String specName, SpecType specType, Boolean isRequired, List<PublicSpecValueRespond> values) {
            super(specId, specCode, specName, specType, isRequired);
            this.values = values;
        }
    }

    /**
     * 用户侧规格值响应 SpecValueRespond
     */
    @Data
    @SuperBuilder
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
    }
}
