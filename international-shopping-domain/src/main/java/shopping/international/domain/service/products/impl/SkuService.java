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
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.products.ISkuService;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SKU 领域服务实现
 *
 * <p>负责协调 SKU 聚合的创建、更新、价格与规格绑定维护以及库存调整, 并同步商品聚合的默认 SKU 与库存总数。</p>
 */
@Service
@RequiredArgsConstructor
public class SkuService implements ISkuService {

    /**
     * SKU 仓储
     */
    private final ISkuRepository skuRepository;
    /**
     * 商品仓储
     */
    private final IProductRepository productRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull List<Sku> list(@NotNull Long productId, @Nullable SkuStatus status) {
        requireProduct(productId);
        return skuRepository.listByProductId(productId, status);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Sku create(@NotNull Long productId, @Nullable String skuCode, @NotNull Integer stock,
                               @Nullable BigDecimal weight, @NotNull SkuStatus status, boolean isDefault,
                               @Nullable String barcode, @NotNull List<ProductPrice> prices,
                               @NotNull List<SkuSpecRelation> specs, @NotNull List<ProductImage> images) {
        requireProduct(productId);
        Sku sku = Sku.create(productId, skuCode, stock, weight, status, isDefault, barcode, prices, specs, images);
        Sku saved = skuRepository.save(sku);
        if (isDefault)
            skuRepository.markDefault(productId, saved.getId());
        refreshProductStock(productId);
        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Sku updateBasic(@NotNull Long productId, @NotNull Long skuId, @Nullable String skuCode,
                                    @Nullable Integer stock, @Nullable BigDecimal weight, @Nullable SkuStatus status,
                                    @Nullable Boolean isDefault, @Nullable String barcode, @Nullable List<ProductImage> images) {
        Sku sku = requireSku(productId, skuId);
        sku.updateBasic(skuCode, weight, status, isDefault, barcode);
        if (stock != null)
            sku.adjustStock(StockAdjustMode.SET, stock);
        if (images != null)
            sku.replaceImages(images);
        Sku updated = skuRepository.updateBasic(sku, images != null);
        if (isDefault != null)
            skuRepository.markDefault(productId, isDefault ? skuId : null);
        if (stock != null)
            refreshProductStock(productId);
        return updated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull List<Long> upsertSpecs(@NotNull Long productId, @NotNull Long skuId, @NotNull List<SkuSpecRelation> specs) {
        Sku sku = requireSku(productId, skuId);
        if (specs.isEmpty())
            return List.of();
        for (SkuSpecRelation relation : specs) {
            boolean exists = sku.getSpecs() != null && sku.getSpecs().stream().anyMatch(item ->
                    Objects.equals(item.getSpecId(), relation.getSpecId())
                            || (item.getSpecCode() != null && item.getSpecCode().equalsIgnoreCase(relation.getSpecCode())));
            if (exists)
                sku.updateSpecSelection(relation);
            else
                sku.addSpecSelection(relation);
        }
        return skuRepository.upsertSpecs(skuId, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteSpec(@NotNull Long productId, @NotNull Long skuId, @NotNull Long specId) {
        requireSku(productId, skuId);
        return skuRepository.deleteSpec(skuId, specId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull List<String> upsertPrices(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPrice> prices) {
        Sku sku = requireSku(productId, skuId);
        if (prices.isEmpty())
            return List.of();
        List<ProductPrice> merged = new ArrayList<>(prices.size());
        for (ProductPrice price : prices) {
            boolean exists = sku.getPrices() != null && sku.getPrices().stream()
                    .anyMatch(item -> item.getCurrency().equals(price.getCurrency()));
            if (exists)
                sku.updatePrice(price.getCurrency(), price.getListPrice(), price.getSalePrice(), price.isActive());
            else
                sku.addPrice(price);
            merged.add(price);
        }
        return skuRepository.upsertPrices(skuId, merged);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int adjustStock(@NotNull Long productId, @NotNull Long skuId,
                           @NotNull StockAdjustMode mode, int quantity) {
        Sku sku = requireSku(productId, skuId);
        sku.adjustStock(mode, quantity);
        int stock = skuRepository.updateStock(skuId, sku.getStock());
        refreshProductStock(productId);
        return stock;
    }

    /**
     * 校验商品是否存在且未删除
     *
     * @param productId 商品 ID
     * @return 商品聚合
     */
    private Product requireProduct(@NotNull Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalParamException("商品不存在"));
        if (product.getStatus() == ProductStatus.DELETED)
            throw new IllegalParamException("商品已删除");
        return product;
    }

    /**
     * 校验 SKU 是否存在且归属指定商品
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @return SKU 聚合
     */
    private Sku requireSku(@NotNull Long productId, @NotNull Long skuId) {
        return skuRepository.findById(productId, skuId)
                .orElseThrow(() -> new IllegalParamException("SKU 不存在"));
    }

    /**
     * 刷新商品聚合库存
     *
     * @param productId 商品 ID
     */
    private void refreshProductStock(@NotNull Long productId) {
        int total = skuRepository.sumStockByProduct(productId);
        productRepository.updateStockTotal(productId, total);
    }
}
