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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品查询服务实现, 负责商品列表与详情的聚合查询, 参数校验及多语言/多币种数据拼装
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService implements IProductQueryService {

    /**
     * 商品查询仓储服务
     */
    private final IProductQueryRepository productQueryRepository;
    /**
     * 商品分类仓储服务
     */
    private final IProductCategoryRepository categoryRepository;
    /**
     * 商品 Like 仓储服务
     */
    private final IProductLikeRepository productLikeRepository;

    /**
     * 查询商品列表并按请求参数进行过滤, 排序与分页
     *
     * <p>方法会对分页, locale, currency, 价格区间, 关键词等参数进行规范化与校验, 同时根据可选的用户 ID 标记用户喜欢的商品</p>
     *
     * @param query 查询参数对象
     * @return 包含商品概要信息的分页结果
     * @throws IllegalParamException 当价格区间或其他参数非法时抛出
     */
    @Override
    public @NotNull PageResult<ProductSummary> list(@NotNull ProductListQuery query) {
        int page = query.page();
        int size = query.size();
        String locale = query.locale() == null || query.locale().isBlank() ? null : query.locale();
        String currency = query.currency() == null || query.currency().isBlank() ? null : query.currency();
        ProductSort sort = query.sortBy() == null ? ProductSort.LATEST : query.sortBy();
        List<String> tagList = query.tags() == null ? Collections.emptyList() : query.tags();
        String keyword = query.keyword();
        BigDecimal priceMin = query.priceMin();
        BigDecimal priceMax = query.priceMax();
        Long categoryId = resolveCategoryId(query.categorySlug(), locale);

        // 主商品分页查询, 副查询依赖该批次的产品 ID/分类 ID
        PageResult<Product> productPageResult = productQueryRepository.pageOnSaleProducts(
                page, size, categoryId, keyword, tagList, locale, currency, priceMin, priceMax, sort);

        if (productPageResult.items().isEmpty())
            return new PageResult<>(Collections.emptyList(), productPageResult.total());

        Set<Long> productIds = productPageResult.items().stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> categoryIds = productPageResult.items().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 根据 productId, locale 获取 productId -> ProductI18n 映射
        Map<Long, ProductI18n> i18nMap = locale == null || productIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapI18nByLocale(productIds, locale);
        // 根据 productId 获取 productId -> List<ProductImage> 映射
        Map<Long, List<ProductImage>> galleryMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapProductImages(productIds);
        // 根据 productId, currency 获取 productId -> ProductPriceRange 映射
        Map<Long, ProductPriceRange> priceRangeMap = currency == null || productIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapPriceRangeByProductIds(productIds, currency);
        // 根据 categoryId 获取 categoryId -> Category 映射
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.mapByIds(categoryIds);
        // 根据 categoryId, locale 获取 categoryId -> CategoryI18n 映射
        Map<Long, CategoryI18n> categoryI18nMap = locale == null || categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.mapI18nByLocale(categoryIds, locale);
        // 根据 userId, productId 获取 userId -> LikedAt 映射
        Map<Long, LocalDateTime> likedAtMap = query.userId() == null || productIds.isEmpty()
                ? Collections.emptyMap()
                : productLikeRepository.mapLikedAt(query.userId(), productIds);

        List<ProductSummary> summaryList = productPageResult.items().stream()
                .map(product -> toSummary(product, i18nMap.get(product.getId()), galleryMap, priceRangeMap,
                        categoryMap, categoryI18nMap, likedAtMap))
                .toList();

        return new PageResult<>(summaryList, productPageResult.total());
    }

    /**
     * 查询单个商品详情, 支持按 locale/currency 返回多语言, 多币种数据
     *
     * <p>会优先使用本地化 slug 查询, 未命中则回退到基础 slug, 并补齐价格, 规格, 图库, 分类, 喜欢状态等信息</p>
     *
     * @param slug        商品唯一标识 (slug)
     * @param locale      期望的语言环境, 允许为空
     * @param currency    期望的币种, 允许为空
     * @param currentUser 当前用户 ID, 用于标记喜欢状态, 允许为空
     * @return 商品详情聚合视图
     * @throws IllegalParamException 当商品不存在或币种下无可售 SKU 时抛出
     */
    @Override
    public @NotNull ProductDetail detail(@NotNull String slug, @Nullable String locale, @Nullable String currency, @Nullable Long currentUser) {
        String normalizedLocale = locale == null || locale.isBlank() ? null : locale;
        String normalizedCurrency = currency == null || currency.isBlank() ? null : currency;

        // 根据本地化后的 slug 和 locale 查询商品, 若未命中则回退到基础 slug, 都查不到则抛出 IllegalParamException
        Optional<Product> localized = normalizedLocale == null
                ? Optional.empty()
                : productQueryRepository.findOnSaleByLocalizedSlug(slug, normalizedLocale);
        Product product = localized.orElseGet(() ->
                productQueryRepository.findOnSaleBySlug(slug)
                        .orElseThrow(() -> new IllegalParamException("商品不存在或未上架"))
        );

        // 根据 ProductId 获取 ProductI18n
        ProductI18n productI18n = normalizedLocale == null
                ? null
                : productQueryRepository.mapI18nByLocale(Set.of(product.getId()), normalizedLocale).get(product.getId());
        // 根据 ProductId 获取其 Category 本体及其本地化
        Category category = categoryRepository.mapByIds(Set.of(product.getCategoryId())).get(product.getCategoryId());
        if (category == null)
            throw new IllegalParamException("商品分类不存在");
        CategoryI18n categoryI18n = normalizedLocale == null
                ? null
                : categoryRepository.mapI18nByLocale(Set.of(product.getCategoryId()), normalizedLocale).get(product.getCategoryId());
        // 根据 ProductId 获取其 商品图片 列表
        List<ProductImage> gallery = productQueryRepository.mapProductImages(Set.of(product.getId()))
                .getOrDefault(product.getId(), Collections.emptyList());

        // 获取 SPU 拥有的规格类别
        List<ProductSpec> specList = productQueryRepository.listSpecs(product.getId());
        // 获取 SPU 拥有的规格值, 并按规格类别 ID 分类
        List<ProductSpecValue> specValueList = productQueryRepository.listSpecValues(product.getId());
        Map<Long, List<ProductSpecValue>> specValueMap = specValueList.stream()
                .collect(Collectors.groupingBy(ProductSpecValue::getSpecId));
        // locale 不为空, 获取他们的 I18N 信息
        if (normalizedLocale != null && !specList.isEmpty()) {
            Map<Long, String> specI18nMap = productQueryRepository.mapSpecI18n(
                    specList.stream()
                            .map(ProductSpec::getId)
                            .collect(Collectors.toSet()),
                    normalizedLocale);
            Map<Long, String> specValueI18nMap = productQueryRepository.mapSpecValueI18n(
                    specValueList.stream()
                            .map(ProductSpecValue::getId)
                            .collect(Collectors.toSet()),
                    normalizedLocale);
            specList.forEach(spec -> spec.applyI18n(specI18nMap.get(spec.getId())));
            specValueList.forEach(value -> value.applyI18n(specValueI18nMap.get(value.getId())));
        }
        // 绑定规格值到规格类别
        specList.forEach(spec -> spec.attachValues(specValueMap.getOrDefault(spec.getId(), Collections.emptyList())));

        // 获取 SPU 拥有的 SKU 列表
        List<ProductSku> skuList = productQueryRepository.listEnabledSkus(product.getId());
        Set<Long> skuIds = skuList.stream()
                .map(ProductSku::getId)
                .collect(Collectors.toSet());
        // 根据 SKU ID, currency 获取 SKU -> SKU价格 映射
        Map<Long, List<ProductPrice>> priceMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapPricesBySkuIds(skuIds, normalizedCurrency);
        // 根据 SKU ID 获取 SKU -> List<ProductImage> 映射
        Map<Long, List<ProductImage>> skuImageMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapSkuImages(skuIds);
        // 根据 SKU ID 获取 SKU -> SKU关联的规格值列表 映射
        Map<Long, List<ProductSkuSpec>> skuSpecMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapSkuSpecs(skuIds);

        // 补充完 图片, 价格, 规格类别, 规格值 等详细状态的 SKU 列表
        List<ProductSku> attachedSkuList = skuList.stream()
                .map(sku -> attachSkuDetails(sku, specList, priceMap, skuImageMap, skuSpecMap, normalizedCurrency))
                .filter(Objects::nonNull)
                .toList();
        if (normalizedCurrency != null && attachedSkuList.isEmpty())
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
                specList,
                attachedSkuList,
                productI18n == null ? List.of() : List.of(productI18n)
        );
    }

    /**
     * 为单个 SKU 注入价格, 图片与规格标签, 同时在无价格时过滤掉不可售的 SKU
     *
     * @param sku         原始 SKU
     * @param specList       规格定义列表, 用于回填规格名称
     * @param priceMap    SKU 价格映射
     * @param skuImageMap SKU 图片映射
     * @param skuSpecMap  SKU 规格映射
     * @param currency    请求的币种, 可空
     * @return 已补充信息的 SKU, 若无价格可售则返回 null
     */
    private ProductSku attachSkuDetails(ProductSku sku,
                                        List<ProductSpec> specList,
                                        Map<Long, List<ProductPrice>> priceMap,
                                        Map<Long, List<ProductImage>> skuImageMap,
                                        Map<Long, List<ProductSkuSpec>> skuSpecMap,
                                        String currency) {
        if (!sku.isEnabled())
            return null;
        List<ProductPrice> prices = priceMap.getOrDefault(sku.getId(), Collections.emptyList());
        if (currency != null && prices.isEmpty())
            return null;
        sku.attachPrices(prices);
        sku.attachImages(skuImageMap.getOrDefault(sku.getId(), Collections.emptyList()));
        List<ProductSkuSpec> rawSkuSpecsMappingList = skuSpecMap.getOrDefault(sku.getId(), Collections.emptyList());
        if (!rawSkuSpecsMappingList.isEmpty()) {
            Map<Long, ProductSpec> specIndex = specList.stream()
                    .collect(Collectors.toMap(ProductSpec::getId, Function.identity()));
            // 遍历原始 SKU 所属的规格(规格值) 列表, 替换为本地化名称 (包括规格类别和规格值)
            List<ProductSkuSpec> adjustedSkuSpecsMappingList = rawSkuSpecsMappingList.stream()
                    .map(raw -> {
                        // 从用 specList 构建的 specIndex 中获取规格分类, 并尝试获取其本地化名称, 若无则使用 SKU 关联的规格的原始名称
                        ProductSpec spec = specIndex.get(raw.getSpecId());
                        String specName = spec != null && spec.getI18nName() != null
                                ? spec.getI18nName()
                                : raw.getSpecName();

                        // 从用 specList 构建的 specIndex 中获取规格分类下的规格值列表
                        // 根据规格值 ID 取出规格值对象, 并尝试获取其本地化名称, 若无则使用 SKU 关联的规格值的原始名称
                        ProductSpecValue value = null;
                        if (specIndex.containsKey(raw.getSpecId()))
                            value = specIndex.get(raw.getSpecId()).getValues()
                                    .stream()
                                    .filter(v -> Objects.equals(v.getId(), raw.getValueId()))
                                    .findFirst()
                                    .orElse(null);
                        String valueName = value != null && value.getI18nName() != null
                                ? value.getI18nName()
                                : raw.getValueName();
                        return ProductSkuSpec.of(raw.getSpecId(), raw.getSpecCode(), specName, raw.getValueId(), raw.getValueCode(), valueName);
                    })
                    .toList();
            sku.attachSpecs(adjustedSkuSpecsMappingList);
        }
        if (currency == null)
            return sku;
        return sku.getPrices().isEmpty() ? null : sku;
    }

    /**
     * 将产品及其相关国际化信息, 图片, 价格范围, 分类等信息转换为产品摘要对象
     *
     * @param product         产品实体
     * @param i18n            产品的国际化信息, 可能为空
     * @param galleryMap      产品图片集合映射, key 为产品 id, value 为该产品的图片列表
     * @param priceRangeMap   产品价格范围映射, key 为产品 id, value 为该产品的价格范围
     * @param categoryMap     分类映射, key 为分类 id, value 为分类对象
     * @param categoryI18nMap 分类的国际化信息映射, key 为分类 id, value 为分类的国际化信息
     * @param likedAtMap      用户喜欢时间映射, key 为产品 id, value 为用户对该产品标记喜欢的时间
     * @return 一个包含产品基本信息和相关附加信息的产品摘要对象
     */
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
        String categorySlug = null;
        if (categoryI18n != null)
            categorySlug = categoryI18n.getSlug();
        else if (category != null)
            categorySlug = category.getSlug();
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

    /**
     * 根据 slug 与可选的 locale 解析分类 ID, 优先本地化 slug, 其次基础 slug
     *
     * @param categorySlug 分类 slug
     * @param locale       请求的语言环境, 允许为空
     * @return 分类 ID, 未找到时抛出异常
     * @throws IllegalParamException 当分类不存在时抛出
     */
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
