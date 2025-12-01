package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductQueryRepository implements IProductQueryRepository {
    /**
     * JSON 序列化/反序列化工具
     */
    private final ObjectMapper objectMapper;
    /**
     * 商品 Mapper
     */
    private final ProductMapper productMapper;
    /**
     * 商品 I18n Mapper
     */
    private final ProductI18nMapper productI18nMapper;
    /**
     * 商品图片 Mapper
     */
    private final ProductImageMapper productImageMapper;
    /**
     * 商品 SKU Mapper
     */
    private final ProductSkuMapper productSkuMapper;
    /**
     * 商品 SKU 图片 Mapper
     */
    private final ProductSkuImageMapper productSkuImageMapper;
    /**
     * 商品价格 Mapper
     */
    private final ProductPriceMapper productPriceMapper;
    /**
     * 商品规格 Mapper
     */
    private final ProductSpecMapper productSpecMapper;
    /**
     * 商品规格 I18n Mapper
     */
    private final ProductSpecI18nMapper productSpecI18nMapper;
    /**
     * 商品规格值 Mapper
     */
    private final ProductSpecValueMapper productSpecValueMapper;
    /**
     * 商品规格值 I18n Mapper
     */
    private final ProductSpecValueI18nMapper productSpecValueI18nMapper;
    /**
     * 商品 SKU 关联的规格 Mapper
     */
    private final ProductSkuSpecMapper productSkuSpecMapper;

    /**
     * 按条件分页查询上架商品
     *
     * @param page       页码, 从1开始
     * @param size       每页数量
     * @param categoryId 分类ID, 可空
     * @param keyword    关键词, 可空
     * @param tags       标签, 可空
     * @param locale     语言, 用于关键字/标签匹配 i18n, 可空
     * @param currency   价格币种, 可空
     * @param priceMin   价格下限, 可空
     * @param priceMax   价格上限, 可空
     * @param sortBy     排序
     * @return 分页结果
     */
    @Override
    public @NotNull IProductQueryService.PageResult<Product> pageOnSaleProducts(int page,
                                                                                int size,
                                                                                @Nullable Long categoryId,
                                                                                @Nullable String keyword,
                                                                                @Nullable List<String> tags,
                                                                                @Nullable String locale,
                                                                                @Nullable String currency,
                                                                                @Nullable BigDecimal priceMin,
                                                                                @Nullable BigDecimal priceMax,
                                                                                @NotNull ProductSort sortBy) {
        int offset = (page - 1) * size;
        List<ProductPO> productPOList = productMapper.selectOnSalePage(
                offset,
                size,
                categoryId,
                keyword,
                tags,
                locale,
                currency,
                priceMin,
                priceMax,
                sortBy.name());
        long total = productMapper.countOnSale(categoryId, keyword, tags, locale, currency, priceMin, priceMax);
        List<Product> productList = productPOList.stream()
                .map(this::toProduct)
                .toList();
        return new IProductQueryService.PageResult<>(productList, total);
    }

    /**
     * 按 slug 查询上架商品
     *
     * @param slug slug
     * @return 商品
     */
    @Override
    public @NotNull Optional<Product> findOnSaleBySlug(@NotNull String slug) {
        ProductPO record = productMapper.selectOnSaleBySlug(slug);
        return Optional.ofNullable(record).map(this::toProduct);
    }

    /**
     * 按指定语言的 slug 查询上架商品
     *
     * @param slug   多语言 slug
     * @param locale 语言
     * @return 商品
     */
    @Override
    public @NotNull Optional<Product> findOnSaleByLocalizedSlug(@NotNull String slug, @NotNull String locale) {
        ProductPO record = productMapper.selectOnSaleByLocalizedSlug(slug, locale);
        return Optional.ofNullable(record).map(this::toProduct);
    }

    /**
     * 按ID查询商品(不限制状态)
     *
     * @param productId 商品ID
     * @return 商品
     */
    @Override
    public @NotNull Optional<Product> findById(@NotNull Long productId) {
        ProductPO record = productMapper.selectById(productId);
        return Optional.ofNullable(record).map(this::toProduct);
    }

    /**
     * 批量查询商品(不限制状态)
     *
     * @param productIds 商品ID集合
     * @return productId -> 商品
     */
    @Override
    public @NotNull Map<Long, Product> mapByIds(@NotNull Set<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductPO> records = productMapper.selectByIds(productIds);
        return records.stream()
                .map(this::toProduct)
                .collect(Collectors.toMap(Product::getId, Function.identity(),
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    /**
     * 批量查询商品 i18n
     *
     * @param productIds 商品ID集合
     * @param locale     语言
     * @return productId -> i18n
     */
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

    /**
     * 批量查询商品图片
     *
     * @param productIds 商品ID集合
     * @return productId -> 图片列表
     */
    @Override
    public @NotNull Map<Long, List<ProductImage>> mapProductImages(@NotNull Set<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductImagePO> records = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .in(ProductImagePO::getProductId, productIds)
                .orderByAsc(ProductImagePO::getSortOrder, ProductImagePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(
                        ProductImagePO::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductImage, Collectors.toList())
                ));
    }

    /**
     * 按币种计算商品价格区间 (仅启用 SKU + 价格)
     *
     * @param productIds 商品ID集合
     * @param currency   币种
     * @return productId -> 价格区间
     */
    @Override
    public @NotNull Map<Long, ProductPriceRange> mapPriceRangeByProductIds(@NotNull Set<Long> productIds, @NotNull String currency) {
        if (productIds.isEmpty())
            return Map.of();
        // 获取一系列 SPU ID 对应的 SKU(ENABLED) 列表
        List<ProductSkuPO> enabledSkuList = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .in(ProductSkuPO::getProductId, productIds)
                .eq(ProductSkuPO::getStatus, SkuStatus.ENABLED.name()));
        // 将 SKU 按 SPU ID 分组
        Map<Long, List<ProductSkuPO>> enabledSkuListOfSpuMap = enabledSkuList.stream()
                .collect(Collectors.groupingBy(ProductSkuPO::getProductId));
        // 获取一系列 SKU ID 对应的价格列表(ACTIVE 价格)
        Set<Long> skuIds = enabledSkuList.stream()
                .map(ProductSkuPO::getId)
                .collect(Collectors.toSet());
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductPricePO> priceList = productPriceMapper.selectList(new LambdaQueryWrapper<ProductPricePO>()
                .eq(ProductPricePO::getCurrency, currency)
                .eq(ProductPricePO::getIsActive, 1)
                .in(ProductPricePO::getSkuId, skuIds));
        Map<Long, List<ProductPricePO>> activePriceListOfSkuMap = priceList.stream()
                .collect(Collectors.groupingBy(ProductPricePO::getSkuId));

        // 得到 SPU -> List<SKU> 和 SKU -> List<价格> 的映射
        Map<Long, ProductPriceRange> result = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ProductSkuPO>> entry : enabledSkuListOfSpuMap.entrySet()) {
            List<ProductPricePO> activePriceList = entry.getValue().stream()
                    .flatMap(sku -> activePriceListOfSkuMap.getOrDefault(sku.getId(), Collections.emptyList()).stream())
                    .toList();
            if (activePriceList.isEmpty())
                continue;
            BigDecimal listMin = null, listMax = null, saleMin = null, saleMax = null;
            for (ProductPricePO price : activePriceList) {
                if (price.getListPrice() == null)
                    continue;
                BigDecimal lp = price.getListPrice();
                listMin = listMin == null ? lp : listMin.min(lp);
                listMax = listMax == null ? lp : listMax.max(lp);
                if (price.getSalePrice() == null)
                    continue;

                BigDecimal sp = price.getSalePrice();
                saleMin = saleMin == null ? sp : saleMin.min(sp);
                saleMax = saleMax == null ? sp : saleMax.max(sp);
            }
            if (listMin != null)
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

    /**
     * 规格 i18n
     *
     * @param specIds 规格ID集合
     * @param locale  语言
     * @return specId -> 名称
     */
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

    /**
     * 规格值 i18n
     *
     * @param valueIds 规格值ID集合
     * @param locale   语言
     * @return valueId -> 名称
     */
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

    /**
     * 查询启用 SKU
     *
     * @param productId 商品ID
     * @return SKU 列表
     */
    @Override
    public @NotNull List<ProductSku> listEnabledSkus(@NotNull Long productId) {
        List<ProductSkuPO> records = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getProductId, productId)
                .eq(ProductSkuPO::getStatus, SkuStatus.ENABLED.name())
                .orderByDesc(ProductSkuPO::getIsDefault)
                .orderByAsc(ProductSkuPO::getId));
        return records.stream().map(this::toProductSku).toList();
    }

    /**
     * 批量查询 SKU 价格 (指定币种)
     *
     * @param skuIds   SKU ID 集合
     * @param currency 币种
     * @return skuId -> 价格
     */
    @Override
    public @NotNull Map<Long, List<ProductPrice>> mapPricesBySkuIds(@NotNull Set<Long> skuIds, @Nullable String currency) {
        if (skuIds.isEmpty())
            return Map.of();
        LambdaQueryWrapper<ProductPricePO> wrapper = new LambdaQueryWrapper<ProductPricePO>()
                .eq(ProductPricePO::getIsActive, 1)
                .in(ProductPricePO::getSkuId, skuIds)
                .orderByDesc(ProductPricePO::getUpdatedAt, ProductPricePO::getId);
        if (currency != null)
            wrapper.eq(ProductPricePO::getCurrency, currency);
        List<ProductPricePO> records = productPriceMapper.selectList(wrapper);
        return records.stream()
                .collect(Collectors.groupingBy(ProductPricePO::getSkuId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductPrice, Collectors.toList())));
    }

    /**
     * SKU 图片
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 图片列表
     */
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

    /**
     * SKU 规格映射
     *
     * @param skuIds SKU ID 集合
     * @return skuId -> 规格值列表
     */
    @Override
    public @NotNull Map<Long, List<ProductSkuSpec>> mapSkuSpecs(@NotNull Set<Long> skuIds) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductSkuSpecPO> records = productSkuSpecMapper.selectList(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .in(ProductSkuSpecPO::getSkuId, skuIds));
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
            ProductSkuSpec skuSpec = ProductSkuSpec.of(spec.getId(), spec.getSpecCode(), spec.getSpecName(),
                    value.getId(), value.getValueCode(), value.getValueName());
            result.computeIfAbsent(po.getSkuId(), ignore -> new ArrayList<>()).add(skuSpec);
        }

        return result.isEmpty() ? Map.of() : result;
    }

    /**
     * 将 ProductPO 对象转换为 Product 对象
     *
     * @param po ProductPO 对象, 包含商品的基本信息
     * @return Product 对象, 表示转换后的完整商品信息
     */
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

    /**
     * 将 ProductI18nPO 转换为 ProductI18n 对象
     *
     * @param po ProductI18nPO 对象, 包含商品的多语言信息
     * @return ProductI18n 对象, 表示商品的多语言覆盖
     */
    private ProductI18n toProductI18n(ProductI18nPO po) {
        return ProductI18n.of(po.getLocale(), po.getTitle(), po.getSubtitle(), po.getDescription(),
                po.getSlug(), parseStringList(po.getTags()));
    }

    /**
     * 将 ProductSkuPO 对象转换为 ProductSku 对象
     *
     * @param po ProductSkuPO 对象, 包含 SKU 的基本信息
     * @return ProductSku 对象, 表示转换后的完整 SKU 信息
     */
    private ProductSku toProductSku(ProductSkuPO po) {
        return ProductSku.reconstitute(
                po.getId(),
                po.getProductId(),
                po.getSkuCode(),
                po.getStock(),
                po.getWeight(),
                SkuStatus.from(po.getStatus()),
                po.getIsDefault() != null && po.getIsDefault() == 1,
                po.getBarcode()
        );
    }

    /**
     * 将 ProductImagePO 转换为 ProductImage 对象
     *
     * @param po ProductImagePO 对象, 包含商品图片的信息
     * @return ProductImage 对象, 表示商品的图片信息
     */
    private ProductImage toProductImage(ProductImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    /**
     * 将 ProductSkuImagePO 对象转换为 ProductImage 对象
     *
     * @param po ProductSkuImagePO 对象, 包含 SKU 图片的信息
     * @return ProductImage 对象, 表示商品的图片信息
     */
    private ProductImage toProductImage(ProductSkuImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    /**
     * 将 ProductPricePO 对象转换为 ProductPrice 对象
     *
     * @param po ProductPricePO 对象, 包含价格信息
     * @return 转换后的 ProductPrice 对象
     * @throws AppException 如果币种或标价为空, 则抛出异常
     */
    private ProductPrice toProductPrice(ProductPricePO po) {
        if (po.getCurrency() == null || po.getListPrice() == null)
            throw new AppException("价格记录缺少必填字段, skuId=" + po.getSkuId());
        return ProductPrice.of(po.getCurrency(), po.getListPrice(), po.getSalePrice(), po.getIsActive() != null && po.getIsActive() == 1);
    }

    /**
     * 将 ProductSpecPO 对象转换为 ProductSpec 对象
     *
     * @param po ProductSpecPO 对象, 包含规格的基本信息
     * @return ProductSpec 对象, 表示转换后的完整规格信息
     */
    private ProductSpec toProductSpec(ProductSpecPO po) {
        return ProductSpec.reconstitute(po.getId(), po.getProductId(), po.getSpecCode(), po.getSpecName(),
                SpecType.from(po.getSpecType()), po.getIsRequired() != null && po.getIsRequired() == 1,
                po.getSortOrder(), !"DISABLED".equalsIgnoreCase(po.getStatus()));
    }

    /**
     * 将 ProductSpecValuePO 对象转换为 ProductSpecValue 对象
     *
     * @param po ProductSpecValuePO 对象, 包含规格值的基本信息
     * @return ProductSpecValue 对象, 表示转换后的完整规格值信息
     */
    private ProductSpecValue toProductSpecValue(ProductSpecValuePO po) {
        return ProductSpecValue.reconstitute(po.getId(), po.getProductId(), po.getSpecId(), po.getValueCode(),
                po.getValueName(), parseAttributes(po.getAttributes()), po.getSortOrder(),
                !"DISABLED".equalsIgnoreCase(po.getStatus()));
    }

    /**
     * 解析给定的 JSON 字符串为字符串列表
     *
     * @param json 要解析的 JSON 字符串, 可为空
     * @return 如果 <code>json</code> 为 null 或空字符串, 则返回空列表; 否则尝试将其解析为字符串列表并返回. 如果解析过程中发生异常, 也返回空列表
     */
    private List<String> parseStringList(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("规格值列表解析失败, 返回空列表, 原始数据: {}", json, ex);
            return Collections.emptyList();
        }
    }

    /**
     * 解析给定的 JSON 字符串为键值对映射
     *
     * @param json 要解析的 JSON 字符串, 可为空
     * @return 如果 <code>json</code> 为 null 或空白字符串, 则返回空映射; 否则尝试将其解析为键值对映射并返回. 如果解析过程中发生异常, 也返回空映射
     */
    private Map<String, Object> parseAttributes(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("规格属性解析失败, 返回空映射, 原始数据: {}", json, ex);
            return Collections.emptyMap();
        }
    }
}
