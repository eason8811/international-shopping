package shopping.international.domain.model.vo.products;

import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.util.List;

/**
 * 商品详情视图
 *
 * @param id            商品ID
 * @param slug          商品别名
 * @param title         商品标题
 * @param subtitle      商品副标题
 * @param description   商品描述
 * @param categoryId    商品分类ID
 * @param categorySlug  商品分类别名
 * @param brand         商品品牌
 * @param coverImageUrl 商品封面图片URL
 * @param stockTotal    商品库存总量
 * @param saleCount     商品销量
 * @param skuType       商品SKU类型
 * @param status        商品状态
 * @param tags          商品标签列表
 * @param defaultSkuId  默认SKU ID
 * @param gallery       商品图片列表
 * @param specs         商品规格列表
 * @param skus          商品SKU列表
 * @param i18n          当前语言的商品国际化信息
 * @param i18nList      商品国际化信息列表
 */
public record ProductDetail(Long id,
                            String slug,
                            String title,
                            String subtitle,
                            String description,
                            Long categoryId,
                            String categorySlug,
                            String brand,
                            String coverImageUrl,
                            int stockTotal,
                            int saleCount,
                            SkuType skuType,
                            ProductStatus status,
                            List<String> tags,
                            Long defaultSkuId,
                            List<ProductImage> gallery,
                            List<ProductSpec> specs,
                            List<ProductSku> skus,
                            ProductI18n i18n,
                            List<ProductI18n> i18nList) {
}
