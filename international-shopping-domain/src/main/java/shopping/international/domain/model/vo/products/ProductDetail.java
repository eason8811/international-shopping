package shopping.international.domain.model.vo.products;

import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;

import java.util.List;

/**
 * 商品详情视图
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
                            ProductI18n i18n) {
}
