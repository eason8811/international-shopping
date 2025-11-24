package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPriceRange;
import shopping.international.domain.model.vo.products.ProductSummary;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品列表响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRespond {
    private Long id;
    private String slug;
    private String title;
    private String subtitle;
    private String description;
    private Long categoryId;
    private String categorySlug;
    private String brand;
    private String coverImageUrl;
    private Integer stockTotal;
    private Integer saleCount;
    private SkuType skuType;
    private ProductStatus status;
    private List<String> tags;
    private PriceRangeRespond priceRange;
    private List<ProductImageRespond> gallery;
    private LocalDateTime likedAt;

    public static ProductRespond from(ProductSummary summary) {
        ProductPriceRange pr = summary.priceRange();
        PriceRangeRespond priceRangeRespond = pr == null ? null : new PriceRangeRespond(
                pr.getCurrency(),
                pr.getListPriceMin(),
                pr.getListPriceMax(),
                pr.getSalePriceMin(),
                pr.getSalePriceMax()
        );
        List<ProductImageRespond> images = summary.gallery() == null ? List.of()
                : summary.gallery().stream().map(ProductImageRespond::from).toList();
        return new ProductRespond(
                summary.id(),
                summary.slug(),
                summary.title(),
                summary.subtitle(),
                summary.description(),
                summary.categoryId(),
                summary.categorySlug(),
                summary.brand(),
                summary.coverImageUrl(),
                summary.stockTotal(),
                summary.saleCount(),
                summary.skuType(),
                summary.status(),
                summary.tags(),
                priceRangeRespond,
                images,
                summary.likedAt()
        );
    }

    /**
     * 价格区间
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceRangeRespond {
        private String currency;
        private java.math.BigDecimal listPriceMin;
        private java.math.BigDecimal listPriceMax;
        private java.math.BigDecimal salePriceMin;
        private java.math.BigDecimal salePriceMax;
    }

    /**
     * 图片
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageRespond {
        private String url;
        private Boolean isMain;
        private Integer sortOrder;

        public static ProductImageRespond from(ProductImage image) {
            return new ProductImageRespond(image.getUrl(), image.isMain(), image.getSortOrder());
        }
    }
}
