package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.ISkuRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.service.products.IProductService;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;
import java.util.Objects;

/**
 * 商品管理领域服务实现
 *
 * <p>编排商品聚合的创建、更新、状态流转、多语言与图库维护, 并提供管理侧分页与详情读取。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    /**
     * 商品聚合仓储
     */
    private final IProductRepository productRepository;
    /**
     * SKU 聚合仓储
     */
    private final ISkuRepository skuRepository;

    /**
     * 分页查询商品
     *
     * @param page           页码, 从 1 开始
     * @param size           页大小
     * @param status         状态过滤, 可空
     * @param skuType        规格类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词, 匹配标题/slug/品牌, 可空
     * @param tag            标签过滤, 可空
     * @param includeDeleted 是否包含已删除商品
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult page(int page, int size, @Nullable ProductStatus status, @Nullable SkuType skuType,
                                    @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag, boolean includeDeleted) {
        int offset = Math.max(0, (page - 1) * size);
        List<Product> items = productRepository.list(
                status,
                skuType,
                categoryId,
                keyword == null ? null : keyword.strip(),
                tag == null ? null : tag.strip(),
                includeDeleted,
                offset,
                size
        );
        long total = productRepository.count(
                status,
                skuType,
                categoryId,
                keyword == null ? null : keyword.strip(),
                tag == null ? null : tag.strip(),
                includeDeleted
        );
        return new PageResult(items, total);
    }

    /**
     * 查询管理侧商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情读模型
     */
    @Override
    public @NotNull ProductDetail detail(@NotNull Long productId) {
        Product product = ensureProduct(productId);
        List<Sku> skus = skuRepository.listByProductId(productId, null);
        String categorySlug = productRepository.findCategorySlug(product.getCategoryId());
        return new ProductDetail(product, categorySlug, skus);
    }

    /**
     * 创建商品基础信息
     *
     * @param slug          商品 slug
     * @param title         标题
     * @param subtitle      副标题
     * @param description   描述
     * @param categoryId    分类 ID
     * @param brand         品牌
     * @param coverImageUrl 主图
     * @param skuType       规格类型
     * @param status        状态
     * @param tags          标签
     * @return 新建商品聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Product createBasic(@NotNull String slug, @NotNull String title, @Nullable String subtitle,
                                        @Nullable String description, @NotNull Long categoryId, @Nullable String brand,
                                        @Nullable String coverImageUrl, @NotNull SkuType skuType,
                                        @NotNull ProductStatus status, @NotNull List<String> tags) {
        Product product = Product.create(slug, title, subtitle, description, categoryId, brand, coverImageUrl,
                skuType, status, tags, List.of(), List.of(), List.of());
        return productRepository.save(product);
    }

    /**
     * 更新商品基础信息
     *
     * @param productId     商品 ID
     * @param slug          新 slug, 可空
     * @param title         新标题, 可空
     * @param subtitle      新副标题, 可空
     * @param description   新描述, 可空
     * @param categoryId    新分类, 可空
     * @param brand         新品牌, 可空
     * @param coverImageUrl 新主图, 可空
     * @param skuType       新规格类型, 可空
     * @param status        新状态, 可空
     * @param tags          新标签, 可空
     * @return 更新后的商品聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Product updateBasic(@NotNull Long productId, @Nullable String slug, @Nullable String title,
                                        @Nullable String subtitle, @Nullable String description, @Nullable Long categoryId,
                                        @Nullable String brand, @Nullable String coverImageUrl, @Nullable SkuType skuType,
                                        @Nullable ProductStatus status, @Nullable List<String> tags) {
        Product product = ensureProduct(productId);
        List<Sku> skus = skuRepository.listByProductId(productId, null);
        product.updateBasic(slug, title, subtitle, description, categoryId, brand, coverImageUrl, skuType, status, tags, skus);
        Product updated = productRepository.updateBasic(product, false);
        if (status != null && (product.getStatus() == ProductStatus.OFF_SHELF || product.getStatus() == ProductStatus.DELETED)) {
            skuRepository.updateStatusByProductId(productId, SkuStatus.DISABLED);
            skuRepository.markDefault(productId, null);
        }
        return updated;
    }

    /**
     * 按合法状态机流转商品状态
     *
     * @param productId 商品 ID
     * @param status    目标状态
     * @return 更新后的状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductStatus changeStatus(@NotNull Long productId, @NotNull ProductStatus status) {
        Product product = ensureProduct(productId);
        List<Sku> skus = skuRepository.listByProductId(productId, null);
        product.changeStatus(status, skus);
        productRepository.updateBasic(product, false);
        if (status == ProductStatus.OFF_SHELF || status == ProductStatus.DELETED) {
            skuRepository.updateStatusByProductId(productId, SkuStatus.DISABLED);
            skuRepository.markDefault(productId, null);
        }
        return product.getStatus();
    }

    /**
     * 新增商品多语言
     *
     * @param productId 商品 ID
     * @param i18n      多语言值对象
     * @return 新增后的多语言
     */
    @Override
    public @NotNull ProductI18n addI18n(@NotNull Long productId, @NotNull ProductI18n i18n) {
        Product product = ensureProduct(productId);
        product.addI18n(i18n);
        productRepository.saveI18n(productId, i18n);
        return i18n;
    }

    /**
     * 更新商品多语言
     *
     * @param productId  商品 ID
     * @param locale     语言代码
     * @param title      新标题, 可空
     * @param subtitle   新副标题, 可空
     * @param description 新描述, 可空
     * @param slug       新 slug, 可空
     * @param tags       新标签, 可空
     * @return 更新后的多语言
     */
    @Override
    public @NotNull ProductI18n updateI18n(@NotNull Long productId, @NotNull String locale, @Nullable String title,
                                           @Nullable String subtitle, @Nullable String description, @Nullable String slug,
                                           @Nullable List<String> tags) {
        Product product = ensureProduct(productId);
        product.updateI18n(locale, title, subtitle, description, slug, tags);
        ProductI18n updated = product.getI18nList().stream()
                .filter(item -> Objects.equals(item.getLocale(), locale))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("商品多语言不存在"));
        productRepository.updateI18n(productId, updated);
        return updated;
    }

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   新图库
     * @return 新图库数量
     */
    @Override
    public int replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        Product product = ensureProduct(productId);
        product.replaceGallery(gallery);
        productRepository.replaceGallery(productId, gallery);
        return gallery.size();
    }

    /**
     * 确认商品存在
     *
     * @param productId 商品 ID
     * @return 商品聚合
     */
    private @NotNull Product ensureProduct(@NotNull Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalParamException("商品不存在"));
    }
}
