package shopping.international.domain.model.vo.products;

import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SPU 简要视图 (非详情)
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
 * @param priceRange    SPU 价格范围
 * @param gallery       商品图片列表
 * @param likedAt       商品收藏时间(如有)
 */
public record ProductSummary(Long id,
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
                             ProductPriceRange priceRange,
                             List<ProductImage> gallery,
                             LocalDateTime likedAt) {
}
