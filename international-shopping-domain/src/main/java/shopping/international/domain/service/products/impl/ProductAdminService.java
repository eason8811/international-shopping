package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductAdminRepository;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductAdminService;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 商品管理服务实现
 */
@Service
@RequiredArgsConstructor
public class ProductAdminService implements IProductAdminService {
    /**
     * slug 最大长度
     */
    private static final int SLUG_MAX = 120;
    /**
     * 标题/副标题最大长度
     */
    private static final int TITLE_MAX = 255;
    /**
     * 品牌最大长度
     */
    private static final int BRAND_MAX = 120;
    /**
     * 图片 URL 最大长度
     */
    private static final int IMAGE_MAX = 500;
    /**
     * 语言代码正则
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");

    /**
     * 商品管理仓储
     */
    private final IProductAdminRepository productAdminRepository;
    /**
     * 分类仓储
     */
    private final IProductCategoryRepository categoryRepository;

    /**
     * 按条件分页筛选商品
     *
     * @param page           页码
     * @param size           每页数量
     * @param status         状态过滤
     * @param skuType        SKU 类型
     * @param categoryId     分类
     * @param keyword        关键词
     * @param tag            标签
     * @param includeDeleted 是否包含已删除
     * @return 商品概要分页结果
     */
    @Override
    public @NotNull PageResult<ProductSummary> page(int page, int size, ProductStatus status, SkuType skuType, Long categoryId, String keyword, String tag, boolean includeDeleted) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedTag = normalizeTag(tag);
        IProductAdminRepository.PageResult<Product> pageResult = productAdminRepository.page(page, size,
                status == null ? null : status.name(),
                skuType == null ? null : skuType.name(),
                categoryId, normalizedKeyword, normalizedTag, includeDeleted);
        if (pageResult.items().isEmpty())
            return new PageResult<>(List.of(), pageResult.total());

        Set<Long> productIds = pageResult.items().stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> categoryIds = pageResult.items().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<ProductImage>> galleryMap = productAdminRepository.mapGallery(productIds);
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of() : categoryRepository.mapByIds(categoryIds);

        List<ProductSummary> items = pageResult.items().stream()
                .map(product -> toSummary(product, categoryMap, galleryMap))
                .toList();
        return new PageResult<>(items, pageResult.total());
    }

    /**
     * 创建商品
     *
     * @param command 保存命令
     * @return 详情
     */
    @Override
    public @NotNull ProductDetail create(@NotNull ProductSaveCommand command) {
        Product normalized = normalizeForSave(null, command);
        Long id = productAdminRepository.insert(normalized);
        return loadDetail(id);
    }

    /**
     * 更新商品
     *
     * @param productId 商品 ID
     * @param command   保存命令
     * @return 详情
     */
    @Override
    public @NotNull ProductDetail update(@NotNull Long productId, @NotNull ProductSaveCommand command) {
        Product existing = productAdminRepository.findById(productId)
                .orElseThrow(() -> IllegalParamException.of("商品不存在"));
        Product normalized = normalizeForSave(existing, command);
        productAdminRepository.update(normalized);
        return loadDetail(productId);
    }

    /**
     * 查询详情
     *
     * @param productId 商品 ID
     * @return 详情
     */
    @Override
    public @NotNull ProductDetail detail(@NotNull Long productId) {
        return loadDetail(productId);
    }

    /**
     * 更新状态
     *
     * @param productId 商品 ID
     * @param status    目标状态
     * @return 详情
     */
    @Override
    public @NotNull ProductDetail updateStatus(@NotNull Long productId, @NotNull ProductStatus status) {
        Product existing = productAdminRepository.findById(productId)
                .orElseThrow(() -> IllegalParamException.of("商品不存在"));
        if (existing.getStatus() == status)
            return loadDetail(productId);
        Product updated = Product.reconstitute(existing.getId(), existing.getSlug(), existing.getTitle(), existing.getSubtitle(),
                existing.getDescription(), existing.getCategoryId(), existing.getBrand(), existing.getCoverImageUrl(),
                existing.getStockTotal(), existing.getSaleCount(), existing.getSkuType(), status,
                existing.getDefaultSkuId(), existing.getTags(), existing.getUpdatedAt());
        productAdminRepository.update(updated);
        return loadDetail(productId);
    }

    /**
     * 批量 upsert 多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     * @return 详情
     */
    @Override
    public @NotNull ProductDetail upsertI18n(@NotNull Long productId, @NotNull List<ProductI18n> payloads) {
        productAdminRepository.findById(productId).orElseThrow(() -> IllegalParamException.of("商品不存在"));
        List<ProductI18n> normalized = normalizeI18n(payloads, productId);
        productAdminRepository.upsertI18n(productId, normalized);
        return loadDetail(productId);
    }

    /**
     * 覆盖图库
     *
     * @param productId 商品 ID
     * @param gallery   图片列表
     * @return 详情
     */
    @Override
    public @NotNull ProductDetail replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        Product existing = productAdminRepository.findById(productId)
                .orElseThrow(() -> IllegalParamException.of("商品不存在"));
        List<ProductImage> normalized = normalizeGallery(gallery);
        productAdminRepository.replaceGallery(productId, normalized);
        // 更新封面图片
        String cover = normalized.stream().filter(ProductImage::isMain).findFirst()
                .map(ProductImage::getUrl)
                .orElse(null);
        Product updated = Product.reconstitute(existing.getId(), existing.getSlug(), existing.getTitle(), existing.getSubtitle(),
                existing.getDescription(), existing.getCategoryId(), existing.getBrand(), cover,
                existing.getStockTotal(), existing.getSaleCount(), existing.getSkuType(), existing.getStatus(),
                existing.getDefaultSkuId(), existing.getTags(), existing.getUpdatedAt());
        productAdminRepository.update(updated);
        return loadDetail(productId);
    }

    /**
     * 组装列表使用的概要视图
     *
     * @param product            商品实体
     * @param categoryMap        分类映射
     * @param galleryMap         图库映射
     * @return 商品概要
     */
    private ProductSummary toSummary(Product product,
                                     Map<Long, Category> categoryMap,
                                     Map<Long, List<ProductImage>> galleryMap) {
        Category category = product.getCategoryId() == null ? null : categoryMap.get(product.getCategoryId());
        List<ProductImage> gallery = galleryMap.getOrDefault(product.getId(), List.of());
        return new ProductSummary(
                product.getId(),
                product.getSlug(),
                product.getTitle(),
                product.getSubtitle(),
                product.getDescription(),
                product.getCategoryId(),
                category != null ? category.getSlug() : null,
                product.getBrand(),
                product.getCoverImageUrl(),
                product.getStockTotal(),
                product.getSaleCount(),
                product.getSkuType(),
                product.getStatus(),
                product.getTags(),
                null,
                gallery,
                null
        );
    }

    /**
     * 将商品保存命令转换为可用于保存的商品对象. 该方法处理了包括但不限于标题, 副标题, 描述, 分类, 品牌等字段的规范化.
     * 此外, 方法还执行了一些基本的业务逻辑验证, 如检查指定的分类是否存在且未被禁用, 以及确保新建商品不能直接设置为删除状态.
     *
     * @param existing 当前已存在的商品实体, 如果是新创建则传入 null
     * @param command 包含了从客户端接收的商品信息的命令对象
     * @return 经过规范化的 <code>Product</code> 对象, 可直接用于持久化存储
     * @throws IllegalParamException 如果提供的参数不符合预期, 比如分类不存在或已被禁用, 或者尝试将一个新商品的状态设置为已删除
     */
    private Product normalizeForSave(Product existing, ProductSaveCommand command) {
        requireNotNull(command, "商品请求不能为空");
        String slug = normalizeSlug(command.getSlug());
        String title = normalizeTitle(command.getTitle());
        String subtitle = normalizeOptional(command.getSubtitle(), TITLE_MAX, "商品副标题长度不能超过 255 个字符");
        String description = command.getDescription() == null ? null : command.getDescription().strip();
        Long categoryId = normalizeCategory(command.getCategoryId());

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> IllegalParamException.of("分类不存在"));
        boolean categoryChanged = existing == null || !Objects.equals(existing.getCategoryId(), categoryId);
        if (category.getStatus() == CategoryStatus.DISABLED && categoryChanged)
            throw new IllegalParamException("分类已停用, 无法创建商品");
        String brand = normalizeOptional(command.getBrand(), BRAND_MAX, "品牌文案长度不能超过 120 个字符");
        String coverImageUrl = normalizeOptional(command.getCoverImageUrl(), IMAGE_MAX, "主图 URL 长度不能超过 500 个字符");
        SkuType skuType = command.getSkuType() == null ? SkuType.SINGLE : command.getSkuType();
        ProductStatus status = command.getStatus() == null
                ? (existing == null ? ProductStatus.DRAFT : existing.getStatus())
                : command.getStatus();
        if (status == ProductStatus.DELETED && existing == null)
            throw new IllegalParamException("新建商品不允许直接标记为删除");

        validateSlugConflict(slug, existing);
        List<String> tags = normalizeTags(command.getTags(), existing == null ? List.of() : existing.getTags());
        return Product.reconstitute(existing == null ? null : existing.getId(), slug, title, subtitle, description,
                categoryId, brand, coverImageUrl,
                existing == null ? 0 : existing.getStockTotal(),
                existing == null ? 0 : existing.getSaleCount(),
                skuType, status,
                existing == null ? null : existing.getDefaultSkuId(),
                tags, existing == null ? null : existing.getUpdatedAt());
    }

    /**
     * 校验 slug 唯一性
     *
     * @param slug     slug
     * @param existing 已有商品
     */
    private void validateSlugConflict(String slug, Product existing) {
        Long excludeId = existing == null ? null : existing.getId();
        if (productAdminRepository.existsSlug(slug, excludeId))
            throw new IllegalParamException("商品 slug 已存在");
    }

    /**
     * 规范化关键字
     *
     * @param keyword 关键字
     * @return 规范化后关键字
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank())
            return null;
        String trimmed = keyword.strip();
        if (trimmed.length() > TITLE_MAX)
            return trimmed.substring(0, TITLE_MAX);
        return trimmed;
    }

    /**
     * 规范化标签筛选
     *
     * @param tag 标签
     * @return 规范化标签
     */
    private String normalizeTag(String tag) {
        if (tag == null || tag.isBlank())
            return null;
        String trimmed = tag.strip();
        if (trimmed.length() > BRAND_MAX)
            return trimmed.substring(0, BRAND_MAX);
        return trimmed;
    }

    /**
     * 规范化 slug
     *
     * @param slug 输入 slug
     * @return 规范化 slug
     */
    private String normalizeSlug(String slug) {
        requireNotBlank(slug, "商品 slug 不能为空");
        String normalized = slug.strip();
        if (normalized.length() > SLUG_MAX)
            throw new IllegalParamException("商品 slug 长度不能超过 120 个字符");
        return normalized;
    }

    /**
     * 规范化标题
     *
     * @param title 标题
     * @return 规范化标题
     */
    private String normalizeTitle(String title) {
        requireNotBlank(title, "商品标题不能为空");
        String normalized = title.strip();
        if (normalized.length() > TITLE_MAX)
            throw new IllegalParamException("商品标题长度不能超过 255 个字符");
        return normalized;
    }

    /**
     * 规范化可选字段
     *
     * @param value     原始值
     * @param maxLength 最大长度
     * @param message   超长提示
     * @return 规范化值
     */
    private String normalizeOptional(String value, int maxLength, String message) {
        if (value == null)
            return null;
        String normalized = value.strip();
        if (normalized.length() > maxLength)
            throw new IllegalParamException(message);
        return normalized;
    }

    /**
     * 规范化分类 ID
     *
     * @param categoryId 分类 ID
     * @return 分类 ID
     */
    private Long normalizeCategory(Long categoryId) {
        requireNotNull(categoryId, "分类 ID 不能为空");
        if (categoryId <= 0)
            throw new IllegalParamException("分类 ID 非法");
        return categoryId;
    }

    /**
     * 规范化标签列表
     *
     * @param tags         请求标签
     * @param fallbackTags 兜底标签
     * @return 规范化列表
     */
    private List<String> normalizeTags(List<String> tags, List<String> fallbackTags) {
        if (tags == null)
            return fallbackTags == null ? List.of() : fallbackTags;
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null)
                continue;
            String trimmed = tag.strip();
            if (trimmed.isEmpty())
                continue;
            if (trimmed.length() > BRAND_MAX)
                throw new IllegalParamException("标签长度不能超过 120 个字符");
            if (seen.add(trimmed))
                normalized.add(trimmed);
        }
        return normalized;
    }

    /**
     * 规范化多语言
     *
     * @param payloads  多语言列表
     * @param productId 商品 ID
     * @return 规范化列表
     */
    private List<ProductI18n> normalizeI18n(List<ProductI18n> payloads, Long productId) {
        if (payloads == null || payloads.isEmpty())
            return List.of();
        List<ProductI18n> normalized = new ArrayList<>();
        Set<String> locales = new LinkedHashSet<>();
        for (ProductI18n payload : payloads) {
            if (payload == null)
                continue;
            if (!locales.add(payload.getLocale()))
                throw new IllegalParamException("重复的多语言 locale");
            if (!LOCALE_PATTERN.matcher(payload.getLocale()).matches())
                throw new IllegalParamException("语言代码格式不合法");
            if (payload.getSlug().length() > SLUG_MAX)
                throw new IllegalParamException("本地化 slug 长度不能超过 120 个字符");
            if (payload.getTitle().length() > TITLE_MAX)
                throw new IllegalParamException("本地化标题长度不能超过 255 个字符");
            if (payload.getSubtitle() != null && payload.getSubtitle().length() > TITLE_MAX)
                throw new IllegalParamException("本地化副标题长度不能超过 255 个字符");
            if (productAdminRepository.existsLocalizedSlug(payload.getLocale(), payload.getSlug(), productId))
                throw new IllegalParamException("本地化 slug 已存在");
            List<String> tags = normalizeTags(payload.getTags(), List.of());
            normalized.add(ProductI18n.of(payload.getLocale(), payload.getTitle(), payload.getSubtitle(),
                    payload.getDescription(), payload.getSlug(), tags));
        }
        return normalized;
    }

    /**
     * 规范化图库
     *
     * @param gallery 图库列表
     * @return 规范化列表
     */
    private List<ProductImage> normalizeGallery(List<ProductImage> gallery) {
        if (gallery == null || gallery.isEmpty())
            return List.of();
        List<ProductImage> normalized = gallery.stream()
                .filter(Objects::nonNull)
                .map(image -> ProductImage.of(image.getUrl(), image.isMain(), image.getSortOrder()))
                .collect(Collectors.toCollection(ArrayList::new));
        boolean hasMain = normalized.stream().anyMatch(ProductImage::isMain);
        // 确保至少有一个主图
        if (!hasMain) {
            ProductImage first = normalized.get(0);
            normalized.set(0, ProductImage.of(first.getUrl(), true, first.getSortOrder()));
        }
        return normalized;
    }

    /**
     * 根据 ID 读取详情
     *
     * @param productId 商品 ID
     * @return 详情
     */
    private ProductDetail loadDetail(Long productId) {
        Product product = productAdminRepository.findById(productId)
                .orElseThrow(() -> IllegalParamException.of("商品不存在"));
        Category category = categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> IllegalParamException.of("商品分类不存在"));
        List<ProductI18n> i18nList = productAdminRepository.mapI18n(Set.of(productId))
                .getOrDefault(productId, List.of());
        ProductI18n primaryI18n = i18nList.isEmpty() ? null : i18nList.get(0);
        List<ProductImage> gallery = productAdminRepository.listGallery(productId);
        return new ProductDetail(
                product.getId(),
                product.getSlug(),
                product.getTitle(),
                product.getSubtitle(),
                product.getDescription(),
                product.getCategoryId(),
                category.getSlug(),
                product.getBrand(),
                product.getCoverImageUrl(),
                product.getStockTotal(),
                product.getSaleCount(),
                product.getSkuType(),
                product.getStatus(),
                product.getTags(),
                product.getDefaultSkuId(),
                gallery,
                List.of(),
                List.of(),
                primaryI18n,
                i18nList
        );
    }
}
