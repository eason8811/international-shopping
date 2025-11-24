package shopping.international.domain.model.vo.products;

import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表视图
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
