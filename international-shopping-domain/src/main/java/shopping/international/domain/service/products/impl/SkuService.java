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
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.products.ISkuService;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 列出商品下的 SKU
     *
     * @param productId 商品 ID
     * @param status    状态过滤, 为空表示不过滤
     * @return SKU 聚合列表
     */
    @Override
    public @NotNull List<Sku> list(@NotNull Long productId, @Nullable SkuStatus status) {
        ensureProduct(productId);
        return skuRepository.listByProductId(productId, status);
    }

    /**
     * 创建 SKU
     *
     * @param productId 所属商品 ID
     * @param skuCode   SKU 编码
     * @param stock     库存
     * @param weight    重量
     * @param status    状态
     * @param isDefault 是否默认
     * @param barcode   条码
     * @param prices    价格列表
     * @param specs     规格绑定
     * @param images    图库
     * @return 新建的聚合
     */
    @Override
    public @NotNull Sku create(@NotNull Long productId, @Nullable String skuCode, @NotNull Integer stock,
                               @Nullable BigDecimal weight, @NotNull SkuStatus status, boolean isDefault,
                               @Nullable String barcode, @NotNull List<ProductPrice> prices,
                               @NotNull List<SkuSpecRelation> specs, @NotNull List<ProductImage> images) {
        ensureProduct(productId);
        Sku sku = Sku.create(productId, skuCode, stock, weight, status, isDefault, barcode, prices, specs, images);
        Sku saved = skuRepository.save(sku);
        if (isDefault)
            skuRepository.markDefault(productId, saved.getId());
        refreshProductStock(productId);
        return saved;
    }

    /**
     * 增量更新 SKU 基础字段
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param skuCode   新编码, 可空
     * @param stock     新库存, 可空
     * @param weight    新重量, 可空
     * @param status    新状态, 可空
     * @param isDefault 默认标记, 可空
     * @param barcode   条码, 可空
     * @param images    新图库, 可空表示不改
     * @return 更新后的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Sku updateBasic(@NotNull Long productId, @NotNull Long skuId, @Nullable String skuCode,
                                    @Nullable Integer stock, @Nullable BigDecimal weight, @Nullable SkuStatus status,
                                    @Nullable Boolean isDefault, @Nullable String barcode, @Nullable List<ProductImage> images) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        sku.updateBasic(skuCode, weight, status, isDefault, barcode);
        if (stock != null)
            sku.adjustStock(StockAdjustMode.SET, stock);
        if (images != null)
            sku.replaceImages(images);

        boolean hasEnabledSkuAfter = product.getStatus() != ProductStatus.ON_SALE
                || skuRepository.existsByProductIdAndStatus(productId, SkuStatus.ENABLED);
        boolean defaultChanged = product.onSkuUpdated(sku, hasEnabledSkuAfter);

        if (isDefault != null) {
            skuRepository.markDefault(productId, isDefault ? skuId : null);
        } else if (defaultChanged) {
            skuRepository.markDefault(productId, product.getDefaultSkuId());
        }

        Sku updated = skuRepository.updateBasic(sku, images != null);
        if (stock != null)
            refreshProductStock(productId);
        return updated;
    }

    /**
     * 增量 upsert 规格绑定
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param specs     规格绑定列表
     * @return 受影响的规格 ID
     */
    @Override
    public @NotNull List<Long> upsertSpecs(@NotNull Long productId, @NotNull Long skuId, @NotNull List<SkuSpecRelation> specs) {
        Sku sku = ensureSku(productId, skuId);
        if (specs.isEmpty())
            return List.of();
        for (SkuSpecRelation relation : specs) {
            boolean exists = sku.getSpecs().stream()
                    .anyMatch(item -> Objects.equals(specKey(item), specKey(relation)));
            if (exists)
                sku.updateSpecSelection(relation);
            else
                sku.addSpecSelection(relation);
        }
        List<SkuSpecRelation> patchedRelations = sku.getSpecs().stream()
                .filter(rel -> specs.stream().anyMatch(req -> Objects.equals(specKey(rel), specKey(req))))
                .toList();
        return skuRepository.upsertSpecs(skuId, patchedRelations);
    }

    /**
     * 解除规格绑定
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteSpec(@NotNull Long productId, @NotNull Long skuId, @NotNull Long specId) {
        ensureSku(productId, skuId);
        return skuRepository.deleteSpec(skuId, specId);
    }

    /**
     * 增量 upsert 价格
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param prices    价格列表
     * @return 受影响的币种
     */
    @Override
    public @NotNull List<String> upsertPrices(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPrice> prices) {
        Sku sku = ensureSku(productId, skuId);
        if (prices.isEmpty())
            return List.of();
        for (ProductPrice price : prices) {
            boolean exists = sku.getPrices().stream()
                    .anyMatch(item -> item.getCurrency().equals(price.getCurrency()));
            if (exists)
                sku.updatePrice(price.getCurrency(), price.getListPrice(), price.getSalePrice(), price.isActive());
            else
                sku.addPrice(price);
        }
        Set<String> affected = prices.stream()
                .map(ProductPrice::getCurrency)
                .collect(Collectors.toSet());
        List<ProductPrice> patchedPrices = sku.getPrices().stream()
                .filter(p -> affected.contains(p.getCurrency()))
                .toList();
        return skuRepository.upsertPrices(skuId, patchedPrices);
    }

    /**
     * 调整库存
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @param mode      调整模式
     * @param quantity  数量
     * @return 调整后的库存
     */
    @Override
    public int adjustStock(@NotNull Long productId, @NotNull Long skuId,
                           @NotNull StockAdjustMode mode, int quantity) {
        Sku sku = ensureSku(productId, skuId);
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
    private Product ensureProduct(@NotNull Long productId) {
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
    private Sku ensureSku(@NotNull Long productId, @NotNull Long skuId) {
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

    /**
     * 提取规格键 (优先 ID, 其次编码)
     */
    private Object specKey(@NotNull SkuSpecRelation relation) {
        return relation.getSpecId() != null ? relation.getSpecId() : relation.getSpecCode();
    }
}
