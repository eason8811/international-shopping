package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.ISkuRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.ProductStatus;
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
     * {@inheritDoc}
     */
    @Override
    public @NotNull PageResult page(int page, int size, @Nullable ProductStatus status, @Nullable SkuType skuType,
                                    @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag, boolean includeDeleted) {
        int offset = Math.max(0, (page - 1) * size);
        List<Product> items = productRepository.list(status, skuType, categoryId, keyword == null ? null : keyword.strip(),
                tag == null ? null : tag.strip(), includeDeleted, offset, size);
        long total = productRepository.count(status, skuType, categoryId, keyword == null ? null : keyword.strip(),
                tag == null ? null : tag.strip(), includeDeleted);
        return new PageResult(items, total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ProductDetail detail(@NotNull Long productId) {
        Product product = ensureProduct(productId);
        List<Sku> skus = skuRepository.listByProductId(productId, null);
        String categorySlug = productRepository.findCategorySlug(product.getCategoryId());
        return new ProductDetail(product, categorySlug, skus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Product createBasic(@NotNull String slug, @NotNull String title, @Nullable String subtitle,
                                        @Nullable String description, @NotNull Long categoryId, @Nullable String brand,
                                        @Nullable String coverImageUrl, @NotNull SkuType skuType,
                                        @NotNull ProductStatus status, @NotNull List<String> tags) {
        Product product = Product.create(slug, title, subtitle, description, categoryId, brand, coverImageUrl,
                skuType, status, tags, List.of(), List.of(), List.of());
        return productRepository.save(product);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Product updateBasic(@NotNull Long productId, @Nullable String slug, @Nullable String title,
                                        @Nullable String subtitle, @Nullable String description, @Nullable Long categoryId,
                                        @Nullable String brand, @Nullable String coverImageUrl, @Nullable SkuType skuType,
                                        @Nullable ProductStatus status, @Nullable List<String> tags) {
        Product product = ensureProduct(productId);
        product.updateBasic(slug, title, subtitle, description, categoryId, brand, coverImageUrl, skuType, status, tags);
        return productRepository.updateBasic(product, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ProductStatus changeStatus(@NotNull Long productId, @NotNull ProductStatus status) {
        Product product = ensureProduct(productId);
        product.changeStatus(status);
        productRepository.updateBasic(product, false);
        return product.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull ProductI18n addI18n(@NotNull Long productId, @NotNull ProductI18n i18n) {
        Product product = ensureProduct(productId);
        product.addI18n(i18n);
        productRepository.saveI18n(productId, i18n);
        return i18n;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
