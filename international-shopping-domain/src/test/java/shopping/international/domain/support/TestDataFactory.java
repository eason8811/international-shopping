package shopping.international.domain.support;

import shopping.international.domain.model.aggregate.products.Category;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.*;
import shopping.international.domain.model.vo.products.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 领域层测试数据工厂, 用于快速构造常用的聚合、实体和值对象。
 */
public final class TestDataFactory {
    private TestDataFactory() {
    }

    public static ProductImage image(String url, boolean main, int sortOrder) {
        return ProductImage.of(url, main, sortOrder);
    }

    public static ProductPrice price(String currency, BigDecimal list, BigDecimal sale, boolean active) {
        return ProductPrice.of(currency, list, sale, active);
    }

    public static ProductSpecValue specValue(Long productId, Long specId, String valueCode, boolean enabled) {
        return ProductSpecValue.create(productId, specId, valueCode, "Value-" + valueCode,
                Map.of("k", "v"), 1, enabled, List.of(ProductSpecValueI18n.of("en-US", "Value-" + valueCode)));
    }

    public static ProductSpec spec(Long productId, Long specId, String code, boolean enabled, ProductSpecValue... values) {
        List<ProductSpecValue> valueList = values.length == 0
                ? List.of(specValue(productId, specId, code + "-1", true))
                : List.of(values);
        return ProductSpec.reconstitute(specId, productId, code, "Spec-" + code, SpecType.COLOR,
                true, 1, enabled, List.of(ProductSpecI18n.of("en-US", "Spec-" + code)), valueList);
    }

    public static Product product(Long id, Long categoryId, SkuType skuType, ProductStatus status, List<ProductSpec> specs) {
        return Product.reconstitute(id, "slug-" + id, "Title-" + id, "Sub-" + id, "Desc-" + id,
                categoryId, "Brand", "cover.jpg", 10, 5, skuType, status, null,
                List.of("tag1", "tag2"), List.of(image("cover.jpg", true, 0)),
                specs, List.of(ProductI18n.of("en-US", "Title-" + id, "Sub-" + id, "Desc-" + id, "slug-" + id, List.of("tag1"))),
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static Sku sku(Long id, Long productId, int stock, boolean defaultSku) {
        return Sku.reconstitute(id, productId, "SKU-" + id, stock, new BigDecimal("1.2"),
                SkuStatus.ENABLED, defaultSku, "BAR-" + id,
                List.of(price("USD", new BigDecimal("9.99"), new BigDecimal("8.99"), true)),
                List.of(SkuSpecRelation.of(1L, "spec-1", "Spec-1", 11L, "value-1", "Value-1")),
                List.of(image("sku-" + id + ".jpg", true, 0)),
                LocalDateTime.now(), LocalDateTime.now());
    }

    public static Category category(Long id, Long parentId, int level, String name, String slug) {
        return Category.reconstitute(id, parentId, name, slug, level, parentId == null ? null : "/%d/".formatted(parentId),
                0, CategoryStatus.ENABLED, "Brand", List.of(CategoryI18n.of("en-US", name, slug, "Brand")),
                LocalDateTime.now(), LocalDateTime.now());
    }
}
