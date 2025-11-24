package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.products.IProductQueryRepository;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.*;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.infrastructure.dao.products.*;
import shopping.international.infrastructure.dao.products.po.*;
import shopping.international.types.exceptions.AppException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品查询仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ProductQueryRepository implements IProductQueryRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProductMapper productMapper;
    private final ProductI18nMapper productI18nMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSkuImageMapper productSkuImageMapper;
    private final ProductPriceMapper productPriceMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductSpecI18nMapper productSpecI18nMapper;
    private final ProductSpecValueMapper productSpecValueMapper;
    private final ProductSpecValueI18nMapper productSpecValueI18nMapper;
    private final ProductSkuSpecMapper productSkuSpecMapper;

    @Override
    public @NotNull IProductQueryService.PageResult<Product> pageOnSaleProducts(int page, int size, @Nullable Long categoryId, @Nullable String keyword, @Nullable List<String> tags, @Nullable String locale, @Nullable String currency, @Nullable BigDecimal priceMin, @Nullable BigDecimal priceMax, @NotNull ProductSort sortBy) {
        int offset = (page - 1) * size;
        List<ProductPO> records = productMapper.selectOnSalePage(offset, size, categoryId, keyword, tags, locale, currency, priceMin, priceMax, sortBy.name());
        long total = productMapper.countOnSale(categoryId, keyword, tags, locale, currency, priceMin, priceMax);
        List<Product> items = records.stream().map(this::toProduct).toList();
        return new IProductQueryService.PageResult<>(items, total);
    }

    @Override
    public @NotNull Optional<Product> findOnSaleBySlug(@NotNull String slug) {
        ProductPO record = productMapper.selectOnSaleBySlug(slug);
        return Optional.ofNullable(record).map(this::toProduct);
    }

    @Override
    public @NotNull Optional<Product> findOnSaleByLocalizedSlug(@NotNull String slug, @NotNull String locale) {
        ProductPO record = productMapper.selectOnSaleByLocalizedSlug(slug, locale);
        return Optional.ofNullable(record).map(this::toProduct);
    }

    @Override
    public @NotNull Map<Long, ProductI18n> mapI18nByLocale(@NotNull Set<Long> productIds, @NotNull String locale) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductI18nPO> records = productI18nMapper.selectList(new LambdaQueryWrapper<ProductI18nPO>()
                .eq(ProductI18nPO::getLocale, locale)
                .in(ProductI18nPO::getProductId, productIds));
        return records.stream()
                .collect(Collectors.toMap(ProductI18nPO::getProductId, this::toProductI18n,
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    @Override
    public @NotNull Map<Long, List<ProductImage>> mapProductImages(@NotNull Set<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductImagePO> records = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .in(ProductImagePO::getProductId, productIds)
                .orderByAsc(ProductImagePO::getSortOrder, ProductImagePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(ProductImagePO::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductImage, Collectors.toList())));
    }

    @Override
    public @NotNull Map<Long, ProductPriceRange> mapPriceRangeByProductIds(@NotNull Set<Long> productIds, @NotNull String currency) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductSkuPO> enabledSkus = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .in(ProductSkuPO::getProductId, productIds)
                .eq(ProductSkuPO::getStatus, SkuStatus.ENABLED.name()));
        Map<Long, List<ProductSkuPO>> skuByProduct = enabledSkus.stream()
                .collect(Collectors.groupingBy(ProductSkuPO::getProductId));
        Set<Long> skuIds = enabledSkus.stream().map(ProductSkuPO::getId).collect(Collectors.toSet());
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductPricePO> prices = productPriceMapper.selectList(new LambdaQueryWrapper<ProductPricePO>()
                .eq(ProductPricePO::getCurrency, currency)
                .eq(ProductPricePO::getIsActive, 1)
                .in(ProductPricePO::getSkuId, skuIds));
        Map<Long, List<ProductPricePO>> priceBySku = prices.stream()
                .collect(Collectors.groupingBy(ProductPricePO::getSkuId));

        Map<Long, ProductPriceRange> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ProductSkuPO>> entry : skuByProduct.entrySet()) {
            List<ProductPricePO> skuPrices = entry.getValue().stream()
                    .flatMap(sku -> priceBySku.getOrDefault(sku.getId(), Collections.emptyList()).stream())
                    .toList();
            if (skuPrices.isEmpty())
                continue;
            BigDecimal listMin = null, listMax = null, saleMin = null, saleMax = null;
            for (ProductPricePO price : skuPrices) {
                if (price.getListPrice() == null)
                    continue;
                BigDecimal lp = price.getListPrice();
                listMin = listMin == null ? lp : listMin.min(lp);
                listMax = listMax == null ? lp : listMax.max(lp);
                if (price.getSalePrice() != null) {
                    BigDecimal sp = price.getSalePrice();
                    saleMin = saleMin == null ? sp : saleMin.min(sp);
                    saleMax = saleMax == null ? sp : saleMax.max(sp);
                }
            }
            if (listMin != null && listMax != null)
                result.put(entry.getKey(), ProductPriceRange.of(currency, listMin, listMax, saleMin, saleMax));
        }
        return result;
    }

    @Override
    public @NotNull List<ProductSpec> listSpecs(@NotNull Long productId) {
        List<ProductSpecPO> records = productSpecMapper.selectList(new LambdaQueryWrapper<ProductSpecPO>()
                .eq(ProductSpecPO::getProductId, productId)
                .eq(ProductSpecPO::getStatus, "ENABLED")
                .orderByAsc(ProductSpecPO::getSortOrder, ProductSpecPO::getId));
        return records.stream().map(this::toProductSpec).toList();
    }

    @Override
    public @NotNull List<ProductSpecValue> listSpecValues(@NotNull Long productId) {
        List<ProductSpecValuePO> records = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getProductId, productId)
                .eq(ProductSpecValuePO::getStatus, "ENABLED")
                .orderByAsc(ProductSpecValuePO::getSortOrder, ProductSpecValuePO::getId));
        return records.stream().map(this::toProductSpecValue).toList();
    }

    @Override
    public @NotNull Map<Long, String> mapSpecI18n(@NotNull Set<Long> specIds, @NotNull String locale) {
        if (specIds.isEmpty())
            return Map.of();
        List<ProductSpecI18nPO> records = productSpecI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecI18nPO>()
                .eq(ProductSpecI18nPO::getLocale, locale)
                .in(ProductSpecI18nPO::getSpecId, specIds));
        return records.stream().collect(Collectors.toMap(ProductSpecI18nPO::getSpecId, ProductSpecI18nPO::getSpecName,
                (existing, ignore) -> existing, LinkedHashMap::new));
    }

    @Override
    public @NotNull Map<Long, String> mapSpecValueI18n(@NotNull Set<Long> valueIds, @NotNull String locale) {
        if (valueIds.isEmpty())
            return Map.of();
        List<ProductSpecValueI18nPO> records = productSpecValueI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                .eq(ProductSpecValueI18nPO::getLocale, locale)
                .in(ProductSpecValueI18nPO::getValueId, valueIds));
        return records.stream().collect(Collectors.toMap(ProductSpecValueI18nPO::getValueId, ProductSpecValueI18nPO::getValueName,
                (existing, ignore) -> existing, LinkedHashMap::new));
    }

    @Override
    public @NotNull List<ProductSku> listEnabledSkus(@NotNull Long productId) {
        List<ProductSkuPO> records = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getProductId, productId)
                .eq(ProductSkuPO::getStatus, SkuStatus.ENABLED.name())
                .orderByDesc(ProductSkuPO::getIsDefault)
                .orderByAsc(ProductSkuPO::getId));
        return records.stream().map(this::toProductSku).toList();
    }

    @Override
    public @NotNull Map<Long, ProductPrice> mapPricesBySkuIds(@NotNull Set<Long> skuIds, @NotNull String currency) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductPricePO> records = productPriceMapper.selectList(new LambdaQueryWrapper<ProductPricePO>()
                .eq(ProductPricePO::getCurrency, currency)
                .eq(ProductPricePO::getIsActive, 1)
                .in(ProductPricePO::getSkuId, skuIds));
        return records.stream()
                .collect(Collectors.toMap(ProductPricePO::getSkuId, this::toProductPrice,
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    @Override
    public @NotNull Map<Long, List<ProductImage>> mapSkuImages(@NotNull Set<Long> skuIds) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductSkuImagePO> records = productSkuImageMapper.selectList(new LambdaQueryWrapper<ProductSkuImagePO>()
                .in(ProductSkuImagePO::getSkuId, skuIds)
                .orderByAsc(ProductSkuImagePO::getSortOrder, ProductSkuImagePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(ProductSkuImagePO::getSkuId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductImage, Collectors.toList())));
    }

    @Override
    public @NotNull Map<Long, List<ProductSkuSpec>> mapSkuSpecs(@NotNull Set<Long> skuIds) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductSkuSpecPO> records = productSkuSpecMapper.selectList(new QueryWrapper<ProductSkuSpecPO>()
                .in("sku_id", skuIds));
        if (records.isEmpty())
            return Map.of();
        Set<Long> specIds = records.stream().map(ProductSkuSpecPO::getSpecId).collect(Collectors.toSet());
        Set<Long> valueIds = records.stream().map(ProductSkuSpecPO::getValueId).collect(Collectors.toSet());
        if (specIds.isEmpty() || valueIds.isEmpty())
            return Map.of();
        Map<Long, ProductSpecPO> specMap = productSpecMapper.selectList(new LambdaQueryWrapper<ProductSpecPO>()
                        .in(ProductSpecPO::getId, specIds)
                        .eq(ProductSpecPO::getStatus, "ENABLED"))
                .stream()
                .collect(Collectors.toMap(ProductSpecPO::getId, Function.identity()));
        Map<Long, ProductSpecValuePO> valueMap = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                        .in(ProductSpecValuePO::getId, valueIds)
                        .eq(ProductSpecValuePO::getStatus, "ENABLED"))
                .stream()
                .collect(Collectors.toMap(ProductSpecValuePO::getId, Function.identity()));

        Map<Long, List<ProductSkuSpec>> result = new LinkedHashMap<>();
        for (ProductSkuSpecPO po : records) {
            ProductSpecPO spec = specMap.get(po.getSpecId());
            ProductSpecValuePO value = valueMap.get(po.getValueId());
            if (spec == null || value == null)
                continue;
            ProductSkuSpec specVo = ProductSkuSpec.of(spec.getId(), spec.getSpecCode(), spec.getSpecName(),
                    value.getId(), value.getValueCode(), value.getValueName());
            result.computeIfAbsent(po.getSkuId(), ignore -> new ArrayList<>()).add(specVo);
        }

        return result.isEmpty() ? Map.of() : result;
    }

    private Product toProduct(ProductPO po) {
        return Product.reconstitute(
                po.getId(),
                po.getSlug(),
                po.getTitle(),
                po.getSubtitle(),
                po.getDescription(),
                po.getCategoryId(),
                po.getBrand(),
                po.getCoverImageUrl(),
                po.getStockTotal(),
                po.getSaleCount(),
                SkuType.from(po.getSkuType()),
                ProductStatus.from(po.getStatus()),
                po.getDefaultSkuId(),
                parseStringList(po.getTags()),
                po.getUpdatedAt()
        );
    }

    private ProductI18n toProductI18n(ProductI18nPO po) {
        return ProductI18n.of(po.getLocale(), po.getTitle(), po.getSubtitle(), po.getDescription(),
                po.getSlug(), parseStringList(po.getTags()));
    }

    private ProductSku toProductSku(ProductSkuPO po) {
        return ProductSku.reconstitute(
                po.getId(),
                po.getSkuCode(),
                po.getStock(),
                po.getWeight(),
                SkuStatus.from(po.getStatus()),
                po.getIsDefault() != null && po.getIsDefault() == 1,
                po.getBarcode()
        );
    }

    private ProductImage toProductImage(ProductImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    private ProductImage toProductImage(ProductSkuImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    private ProductPrice toProductPrice(ProductPricePO po) {
        if (po.getCurrency() == null || po.getListPrice() == null)
            throw new AppException("价格记录缺少必填字段, skuId=" + po.getSkuId());
        return ProductPrice.of(po.getCurrency(), po.getListPrice(), po.getSalePrice(), po.getIsActive() != null && po.getIsActive() == 1);
    }

    private ProductSpec toProductSpec(ProductSpecPO po) {
        return ProductSpec.reconstitute(po.getId(), po.getProductId(), po.getSpecCode(), po.getSpecName(),
                SpecType.from(po.getSpecType()), po.getIsRequired() != null && po.getIsRequired() == 1);
    }

    private ProductSpecValue toProductSpecValue(ProductSpecValuePO po) {
        return ProductSpecValue.reconstitute(po.getId(), po.getSpecId(), po.getValueCode(), po.getValueName(), parseAttributes(po.getAttributes()));
    }

    private List<String> parseStringList(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseAttributes(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyMap();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ignore) {
            return Collections.emptyMap();
        }
    }
}
