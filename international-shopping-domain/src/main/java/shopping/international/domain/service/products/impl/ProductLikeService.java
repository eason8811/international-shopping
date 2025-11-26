package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.adapter.repository.products.IProductLikeRepository;
import shopping.international.domain.adapter.repository.products.IProductQueryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductLike;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductLikeService;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 商品点赞领域服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLikeService implements IProductLikeService {

    /**
     * 语言代码正则表达式
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
    /**
     * 货币代码正则表达式
     */
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Za-z]{3}$");
    /**
     * 默认最大分页大小
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 商品点赞仓储服务
     */
    private final IProductLikeRepository productLikeRepository;
    /**
     * 商品查询仓储服务
     */
    private final IProductQueryRepository productQueryRepository;
    /**
     * 商品分类仓储服务
     */
    private final IProductCategoryRepository categoryRepository;

    /**
     * 点赞商品 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 点赞状态
     */
    @Override
    public @NotNull LikeState like(@NotNull Long userId, @NotNull Long productId) {
        Product product = requireProductExist(productId);
        if (product.getStatus() == ProductStatus.DELETED)
            throw new IllegalParamException("商品不存在或已删除");
        LocalDateTime likedAt = productLikeRepository.like(userId, productId);
        return LikeState.liked(likedAt);
    }

    /**
     * 取消点赞商品 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 点赞状态
     */
    @Override
    public @NotNull LikeState unlike(@NotNull Long userId, @NotNull Long productId) {
        boolean influenced = productLikeRepository.unlike(userId, productId);
        if (!influenced)
            log.warn("取消点赞商品失败, userId={}, productId={}", userId, productId);
        return LikeState.unliked();
    }

    /**
     * 查询用户点赞的商品列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param size     每页数量
     * @param locale   语言
     * @param currency 价格币种
     * @return 点赞的商品列表
     */
    @Override
    public @NotNull IProductQueryService.PageResult<ProductSummary> listUserLikes(@NotNull Long userId, int page, int size,
                                                                                  @Nullable String locale, @Nullable String currency) {
        int pageNo = page <= 0 ? 1 : page;
        int pageSize = size <= 0 ? 10 : Math.min(size, MAX_PAGE_SIZE);
        // 规范化 locale 和 currency
        String normalizedLocale = normalizeLocale(locale);
        String normalizedCurrency = normalizeCurrency(currency);

        // 获取该 userId 的点赞信息列表 List<user_id, product_id, liked_at>
        IProductLikeRepository.PageResult likePage = productLikeRepository.pageLikes(userId, pageNo, pageSize);
        if (likePage.items().isEmpty())
            return new IProductQueryService.PageResult<>(Collections.emptyList(), likePage.total());

        // 转换成 productId -> likedAt 的 Map
        LinkedHashMap<Long, LocalDateTime> likedAtMap = likePage.items()
                .stream()
                .collect(Collectors.toMap(ProductLike::productId, ProductLike::likedAt,
                        (existing, ignore) -> existing, LinkedHashMap::new));
        // 转换成 productId -> Product 的 Map
        Set<Long> productIds = new LinkedHashSet<>(likedAtMap.keySet());
        Map<Long, Product> productMap = productQueryRepository.mapByIds(productIds);

        if (productMap.isEmpty())
            return new IProductQueryService.PageResult<>(Collections.emptyList(), likePage.total());

        Set<Long> missingIds = productIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missingIds.isEmpty())
            log.warn("用户点赞的商品存在缺失记录, userId={}, missingProductIds={}", userId, missingIds);

        // 从 Product 列表中获取每个商品的 CategoryId 并组成集合
        Set<Long> categoryIds = productMap.values().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 根据 productId, locale 获取 productId -> ProductI18n 映射
        Map<Long, ProductI18n> i18nMap = normalizedLocale == null || productMap.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapI18nByLocale(productMap.keySet(), normalizedLocale);
        // 根据 productId 获取 productId -> List<ProductImage> 映射
        Map<Long, List<ProductImage>> galleryMap = productMap.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapProductImages(productMap.keySet());
        // 根据 productId, currency 获取 productId -> ProductPriceRange 映射
        Map<Long, ProductPriceRange> priceRangeMap = normalizedCurrency == null || productMap.isEmpty()
                ? Collections.emptyMap()
                : productQueryRepository.mapPriceRangeByProductIds(productMap.keySet(), normalizedCurrency);
        // 根据 categoryId 获取 categoryId -> Category 映射
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.mapByIds(categoryIds);
        // 根据 categoryId, locale 获取 categoryId -> CategoryI18n 映射
        Map<Long, CategoryI18n> categoryI18nMap = normalizedLocale == null || categoryIds.isEmpty()
                ? Collections.emptyMap()
                : categoryRepository.mapI18nByLocale(categoryIds, normalizedLocale);

        List<ProductSummary> summaryList = likePage.items().stream()
                .map(like -> {
                    Product product = productMap.get(like.productId());
                    if (product == null || product.getStatus() == ProductStatus.DELETED)
                        return null;
                    ProductI18n productI18n = i18nMap.get(product.getId());
                    return toSummary(product, productI18n, galleryMap, priceRangeMap, categoryMap, categoryI18nMap, likedAtMap);
                })
                .filter(Objects::nonNull)
                .toList();

        return new IProductQueryService.PageResult<>(summaryList, likePage.total());
    }

    /**
     * 构建商品摘要
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
     * 检查并确保指定的商品存在 如果不存在则抛出异常
     *
     * @param productId 商品的唯一标识符 用于查询商品是否存在
     * @return 存在的商品实体 如果找不到对应的商品 则抛出异常
     * @throws IllegalParamException 当提供的 productId 对应的商品不存在时抛出此异常
     */
    private Product requireProductExist(Long productId) {
        return productQueryRepository.findById(productId)
                .orElseThrow(() -> new IllegalParamException("商品不存在"));
    }

    /**
     * 规范化 locale
     */
    private String normalizeLocale(@Nullable String locale) {
        if (locale == null)
            return null;
        String trimmed = locale.trim();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() > 16)
            throw new IllegalParamException("locale 过长");
        if (!LOCALE_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("locale 格式不正确");
        return trimmed;
    }

    /**
     * 规范化 currency
     */
    private String normalizeCurrency(@Nullable String currency) {
        if (currency == null)
            return null;
        String trimmed = currency.trim().toUpperCase();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.length() != 3 || !CURRENCY_PATTERN.matcher(trimmed).matches())
            throw new IllegalParamException("currency 格式不正确");
        return trimmed;
    }
}
