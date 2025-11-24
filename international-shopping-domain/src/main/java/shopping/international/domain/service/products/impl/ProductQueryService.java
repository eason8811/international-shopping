package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.adapter.repository.products.IProductLikeRepository;
import shopping.international.domain.adapter.repository.products.IProductQueryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductSort;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 商品查询服务实现
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService implements IProductQueryService {

    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");

    private final IProductQueryRepository productQueryRepository;
    private final IProductCategoryRepository categoryRepository;
    private final IProductLikeRepository productLikeRepository;

    @Override
    public @NotNull PageResult<ProductSummary> list(@NotNull ProductListQuery query) {
        int page = query.page() <= 0 ? 1 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        String locale = normalizeLocale(query.locale());
        String currency = normalizeCurrency(query.currency());
        ProductSort sort = query.sortBy() == null ? ProductSort.LATEST : query.sortBy();
        List<String> tags = normalizeTags(query.tags());
        String keyword = normalizeKeyword(query.keyword());

        BigDecimal priceMin = normalizePrice(query.priceMin());
        BigDecimal priceMax = normalizePrice(query.priceMax());
        if ((priceMin != null || priceMax != null) && currency == null)
            throw new IllegalParamException("按价格筛选时必须提供 currency");
        if (priceMin != null && priceMax != null && priceMin.compareTo(priceMax) > 0)
            throw new IllegalParamException("价格区间不合法");

        Long categoryId = resolveCategoryId(query.categorySlug(), locale);

        PageResult<Product> productPage = productQueryRepository.pageOnSaleProducts(
                page, size, categoryId, keyword, tags, locale, currency, priceMin, priceMax, sort);

        if (productPage.items().isEmpty())
            return new PageResult<>(Collections.emptyList(), productPage.total());

        Set<Long> productIds = productPage.items().stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> categoryIds = productPage.items().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ProductI18n> i18nMap = locale == null || productIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapI18nByLocale(productIds, locale);
        Map<Long, List<ProductImage>> galleryMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapProductImages(productIds);
        Map<Long, ProductPriceRange> priceRangeMap = currency == null || productIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapPriceRangeByProductIds(productIds, currency);
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.mapByIds(categoryIds);
        Map<Long, CategoryI18n> categoryI18nMap = locale == null || categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.mapI18nByLocale(categoryIds, locale);
        Map<Long, LocalDateTime> likedAtMap = query.userId() == null || productIds.isEmpty()
                ? Collections.emptyMap()
                : productLikeRepository.mapLikedAt(query.userId(), productIds);

        List<ProductSummary> summaries = productPage.items().stream()
                .map(product -> toSummary(product, i18nMap.get(product.getId()), galleryMap, priceRangeMap,
                        categoryMap, categoryI18nMap, likedAtMap))
                .toList();

        return new PageResult<>(summaries, productPage.total());
    }

    @Override
    public @NotNull ProductDetail detail(@NotNull String slug, @Nullable String locale, @Nullable String currency, @Nullable Long currentUser) {
        String normalizedLocale = normalizeLocale(locale);
        String normalizedCurrency = normalizeCurrency(currency);

        Optional<Product> localized = normalizedLocale == null
                ? Optional.empty()
                : productQueryRepository.findOnSaleByLocalizedSlug(slug, normalizedLocale);
        Product product = localized.orElseGet(() -> productQueryRepository.findOnSaleBySlug(slug)
                .orElseThrow(() -> new IllegalParamException("商品不存在或未上架")));

        ProductI18n productI18n = normalizedLocale == null
                ? null
                : productQueryRepository.mapI18nByLocale(Set.of(product.getId()), normalizedLocale).get(product.getId());

        Category category = categoryRepository.mapByIds(Set.of(product.getCategoryId()))
                .get(product.getCategoryId());
        if (category == null)
            throw new IllegalParamException("商品分类不存在");
        CategoryI18n categoryI18n = normalizedLocale == null
                ? null
                : categoryRepository.mapI18nByLocale(Set.of(product.getCategoryId()), normalizedLocale)
                .get(product.getCategoryId());

        List<ProductImage> gallery = productQueryRepository.mapProductImages(Set.of(product.getId()))
                .getOrDefault(product.getId(), Collections.emptyList());

        List<ProductSpec> specs = productQueryRepository.listSpecs(product.getId());
        List<ProductSpecValue> specValues = productQueryRepository.listSpecValues(product.getId());
        Map<Long, List<ProductSpecValue>> specValueMap = specValues.stream()
                .collect(Collectors.groupingBy(ProductSpecValue::getSpecId));
        if (normalizedLocale != null && !specs.isEmpty()) {
            Map<Long, String> specI18nMap = productQueryRepository.mapSpecI18n(specs.stream()
                    .map(ProductSpec::getId).collect(Collectors.toSet()), normalizedLocale);
            Map<Long, String> specValueI18nMap = productQueryRepository.mapSpecValueI18n(specValues.stream()
                    .map(ProductSpecValue::getId).collect(Collectors.toSet()), normalizedLocale);
            specs.forEach(spec -> spec.applyI18n(specI18nMap.get(spec.getId())));
            specValues.forEach(value -> value.applyI18n(specValueI18nMap.get(value.getId())));
        }
        specs.forEach(spec -> spec.attachValues(specValueMap.getOrDefault(spec.getId(), Collections.emptyList())));

        List<ProductSku> skus = productQueryRepository.listEnabledSkus(product.getId());
        Set<Long> skuIds = skus.stream().map(ProductSku::getId).collect(Collectors.toSet());
        Map<Long, ProductPrice> priceMap = normalizedCurrency == null || skuIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapPricesBySkuIds(skuIds, normalizedCurrency);
        Map<Long, List<ProductImage>> skuImageMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapSkuImages(skuIds);
        Map<Long, List<ProductSkuSpec>> skuSpecMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapSkuSpecs(skuIds);

        List<ProductSku> enrichedSkus = skus.stream()
                .map(sku -> attachSkuDetails(sku, specs, priceMap, skuImageMap, skuSpecMap))
                .filter(Objects::nonNull)
                .toList();
        if (normalizedCurrency != null && enrichedSkus.isEmpty())
            throw new IllegalParamException("该商品在指定币种下无可售 SKU 价格");

        List<String> tags = productI18n != null && !productI18n.getTags().isEmpty()
                ? productI18n.getTags()
                : product.getTags();

        return new ProductDetail(
                product.getId(),
                productI18n != null ? productI18n.getSlug() : product.getSlug(),
                productI18n != null ? productI18n.getTitle() : product.getTitle(),
                productI18n != null ? productI18n.getSubtitle() : product.getSubtitle(),
                productI18n != null ? productI18n.getDescription() : product.getDescription(),
                product.getCategoryId(),
                categoryI18n != null ? categoryI18n.getSlug() : category.getSlug(),
                product.getBrand(),
                product.getCoverImageUrl(),
                product.getStockTotal(),
                product.getSaleCount(),
                product.getSkuType(),
                product.getStatus(),
                tags,
                product.getDefaultSkuId(),
                gallery,
                specs,
                enrichedSkus,
                productI18n
        );
    }

    private ProductSku attachSkuDetails(ProductSku sku,
                                        List<ProductSpec> specs,
                                        Map<Long, ProductPrice> priceMap,
                                        Map<Long, List<ProductImage>> skuImageMap,
                                        Map<Long, List<ProductSkuSpec>> skuSpecMap) {
        if (!sku.isEnabled())
            return null;
        ProductPrice price = priceMap.get(sku.getId());
        if (price != null)
            sku.attachPrice(price);
        sku.attachImages(skuImageMap.getOrDefault(sku.getId(), Collections.emptyList()));
        List<ProductSkuSpec> rawSpecs = skuSpecMap.getOrDefault(sku.getId(), Collections.emptyList());
        if (!rawSpecs.isEmpty()) {
            Map<Long, ProductSpec> specIndex = specs.stream()
                    .collect(Collectors.toMap(ProductSpec::getId, s -> s));
            List<ProductSkuSpec> adjusted = rawSpecs.stream()
                    .map(raw -> {
                        ProductSpec spec = specIndex.get(raw.getSpecId());
                        String specName = spec != null && spec.getI18nName() != null ? spec.getI18nName() : raw.getSpecName();
                        ProductSpecValue value = specIndex.containsKey(raw.getSpecId())
                                ? specIndex.get(raw.getSpecId()).getValues().stream()
                                .filter(v -> Objects.equals(v.getId(), raw.getValueId()))
                                .findFirst().orElse(null)
                                : null;
                        String valueName = value != null && value.getI18nName() != null ? value.getI18nName() : raw.getValueName();
                        return ProductSkuSpec.of(raw.getSpecId(), raw.getSpecCode(), specName, raw.getValueId(), raw.getValueCode(), valueName);
                    })
                    .toList();
            sku.attachSpecs(adjusted);
        }
        if (priceMap.isEmpty())
            return sku;
        return sku.getPrice() == null ? null : sku;
    }

    private ProductSummary toSummary(Product product,
                                     ProductI18n i18n,
                                     Map<Long, List<ProductImage>> galleryMap,
                                     Map<Long, ProductPriceRange> priceRangeMap,
                                     Map<Long, Category> categoryMap,
                                     Map<Long, CategoryI18n> categoryI18nMap,
                                     Map<Long, LocalDateTime> likedAtMap) {
        String displayTitle = i18n != null ? i18n.getTitle() : product.getTitle();
        String displaySubtitle = i18n != null ? i18n.getSubtitle() : product.getSubtitle();
        String displayDescription = i18n != null ? i18n.getDescription() : product.getDescription();
        String displaySlug = i18n != null ? i18n.getSlug() : product.getSlug();
        List<String> displayTags = i18n != null && !i18n.getTags().isEmpty() ? i18n.getTags() : product.getTags();
        Category category = categoryMap.get(product.getCategoryId());
        CategoryI18n categoryI18n = categoryI18nMap.get(product.getCategoryId());
        String categorySlug = categoryI18n != null
                ? categoryI18n.getSlug()
                : category != null ? category.getSlug() : null;
        List<ProductImage> gallery = galleryMap.getOrDefault(product.getId(), Collections.emptyList());
        ProductPriceRange priceRange = priceRangeMap.get(product.getId());
        LocalDateTime likedAt = likedAtMap.get(product.getId());

        return new ProductSummary(
                product.getId(),
                displaySlug,
                displayTitle,
                displaySubtitle,
                displayDescription,
                product.getCategoryId(),
                categorySlug,
                product.getBrand(),
                product.getCoverImageUrl(),
                product.getStockTotal(),
                product.getSaleCount(),
                product.getSkuType(),
                product.getStatus(),
                displayTags,
                priceRange,
                gallery,
                likedAt
        );
    }

    private String normalizeLocale(@Nullable String locale) {
        if (locale == null)
            return null;
        String trimmed = locale.trim();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() > 16)
            throw new IllegalParamException("locale 最长 16 个字符");
        if (!LOCALE_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("locale 格式不合法");
        return trimmed;
    }

    private String normalizeCurrency(@Nullable String currency) {
        if (currency == null)
            return null;
        String trimmed = currency.trim().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty())
            return null;
        if (!CURRENCY_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("currency 需为 3 位字母代码");
        return trimmed;
    }

    private List<String> normalizeTags(@Nullable List<String> tags) {
        if (tags == null || tags.isEmpty())
            return Collections.emptyList();
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
    }

    private String normalizeKeyword(@Nullable String keyword) {
        if (keyword == null)
            return null;
        String trimmed = keyword.trim();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() > 120)
            throw new IllegalParamException("关键词长度不能超过 120");
        return trimmed;
    }

    private BigDecimal normalizePrice(@Nullable BigDecimal price) {
        if (price == null)
            return null;
        if (price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalParamException("价格不能为负");
        return price;
    }

    private Long resolveCategoryId(@Nullable String categorySlug, @Nullable String locale) {
        if (categorySlug == null || categorySlug.isBlank())
            return null;
        String trimmed = categorySlug.trim();
        Optional<Category> localized = locale == null
                ? Optional.empty()
                : categoryRepository.findByLocalizedSlug(trimmed, locale);
        Optional<Category> base = categoryRepository.findBySlug(trimmed);
        return localized.or(() -> base)
                .map(Category::getId)
                .orElseThrow(() -> new IllegalParamException("分类不存在"));
    }
}
