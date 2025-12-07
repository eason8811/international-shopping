package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.ISkuRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;

/**
 * 商品查询领域服务实现
 *
 * <p>按 slug 聚合商品、规格与 SKU 数据, 并进行必要的状态过滤和本地化准备。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductQueryService implements IProductQueryService {

    /**
     * 商品仓储
     */
    private final IProductRepository productRepository;
    /**
     * SKU 仓储
     */
    private final ISkuRepository skuRepository;

    /**
     * 按 slug 查询用户可见的商品详情
     *
     * @param slug     商品 slug 或本地化 slug
     * @param locale   目标语言
     * @param currency 目标币种, 用于价格过滤
     * @return 商品详情读模型
     */
    @Override
    public @NotNull ProductDetail getPublicDetail(@NotNull String slug, @NotNull String locale, @NotNull String currency) {
        Product product = productRepository.findOnSaleBySlug(slug, locale)
                .orElseThrow(() -> new IllegalParamException("商品不存在或未上架"));
        if (product.getStatus() != ProductStatus.ON_SALE)
            throw new IllegalParamException("商品未上架");
        Product filteredProduct = filterProduct(product);
        List<Sku> skus = skuRepository.listByProductId(product.getId(), SkuStatus.ENABLED);
        List<Sku> filteredSkus = filterSkuPrices(skus, currency);
        String categorySlug = productRepository.findCategorySlug(product.getCategoryId());
        return new ProductDetail(filteredProduct, categorySlug, filteredSkus);
    }

    /**
     * 仅保留启用规格和值
     *
     * @param product 商品聚合
     * @return 过滤后的商品聚合
     */
    private Product filterProduct(@NotNull Product product) {
        List<ProductSpec> specs = product.getSpecs();
        List<ProductSpec> enabledSpecs = specs.stream()
                .filter(ProductSpec::isEnabled)
                .map(this::filterSpecValues)
                .toList();
        return Product.reconstitute(
                product.getId(), product.getSlug(), product.getTitle(), product.getSubtitle(),
                product.getDescription(), product.getCategoryId(), product.getBrand(), product.getCoverImageUrl(),
                product.getStockTotal(), product.getSaleCount(), product.getSkuType(), product.getStatus(),
                product.getDefaultSkuId(), product.getTags(), product.getGallery(), enabledSpecs,
                product.getI18nList(), product.getCreatedAt(), product.getUpdatedAt()
        );
    }

    /**
     * 过滤规格下的启用值
     *
     * @param spec 规格实体
     * @return 过滤后的规格
     */
    private ProductSpec filterSpecValues(@NotNull ProductSpec spec) {
        List<ProductSpecValue> values = spec.getValues();
        List<ProductSpecValue> enabledValues = values.stream()
                .filter(ProductSpecValue::isEnabled)
                .toList();
        return ProductSpec.reconstitute(spec.getId(), spec.getProductId(), spec.getSpecCode(), spec.getSpecName(),
                spec.getSpecType(), spec.isRequired(), spec.getSortOrder(), spec.isEnabled(),
                spec.getI18nList(), enabledValues);
    }

    /**
     * 按币种过滤 SKU 价格
     *
     * @param skus     SKU 列表
     * @param currency 目标币种
     * @return 过滤后的 SKU 列表
     */
    private List<Sku> filterSkuPrices(@NotNull List<Sku> skus, @Nullable String currency) {
        if (currency == null)
            return skus;
        return skus.stream()
                .map(sku -> rebuildSkuWithCurrency(sku, currency))
                .toList();
    }

    /**
     * 按币种重建 SKU
     *
     * @param sku      原始 SKU
     * @param currency 目标币种
     * @return 新 SKU
     */
    private Sku rebuildSkuWithCurrency(@NotNull Sku sku, @NotNull String currency) {
        List<shopping.international.domain.model.vo.products.ProductPrice> filteredPrices = sku.getPrices()
                .stream()
                .filter(price -> currency.equalsIgnoreCase(price.getCurrency()))
                .toList();
        return Sku.reconstitute(sku.getId(), sku.getProductId(), sku.getSkuCode(), sku.getStock(),
                sku.getWeight(), sku.getStatus(), sku.isDefaultSku(), sku.getBarcode(),
                filteredPrices, sku.getSpecs(), sku.getImages(), sku.getCreatedAt(), sku.getUpdatedAt());
    }

    // 留空: 规格和规格值的本地化在触发层根据读取到的 i18n 数据完成
}
