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
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.domain.service.products.ISkuService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
        ensureSkuSpecRelationValidate(productId, specs);
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
        if (SkuStatus.ENABLED == status)
            ensureSkuSpecRelationValidate(productId, sku.getSpecs());
        if (stock != null)
            sku.adjustStock(StockAdjustMode.SET, stock);
        if (images != null)
            sku.replaceImages(images);

        Sku updated = skuRepository.updateBasic(sku, images != null);
        if (stock != null)
            refreshProductStock(productId);

        boolean hasEnabledSkuAfter = skuRepository.existsByProductIdAndStatus(productId, SkuStatus.ENABLED);
        boolean defaultChanged = product.onSkuUpdated(sku, hasEnabledSkuAfter);

        if (defaultChanged)
            skuRepository.markDefault(productId, product.getDefaultSkuId());
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

        sku.patchSpecSelection(specs);
        List<SkuSpecRelation> newSpecRelationList = sku.getSpecs();
        ensureSkuSpecRelationValidate(productId, newSpecRelationList);

        return skuRepository.upsertSpecs(skuId, sku.getSpecs());
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
     * 删除指定商品下的 SKU
     *
     * @param productId 所属商品 ID
     * @param skuId     要删除的 SKU ID
     * @return 如果删除成功返回 <code>true</code>, 否则返回 <code>false</code>
     */
    @Override
    public boolean delete(Long productId, Long skuId) {
        Product product = ensureProduct(productId);
        Sku sku = ensureSku(productId, skuId);
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        Map<Long, SkuStatus> statusBySkuIdMap = skuList.stream().collect(Collectors.toMap(Sku::getId, Sku::getStatus));
        statusBySkuIdMap.put(skuId, SkuStatus.DISABLED);
        boolean hasEnabledSkuAfter = statusBySkuIdMap.values().stream().anyMatch(status -> status == SkuStatus.ENABLED);
        boolean defaultChanged = product.onSkuUpdated(sku, hasEnabledSkuAfter);
        if (defaultChanged)
            skuRepository.markDefault(productId, product.getDefaultSkuId());

        return skuRepository.delete(productId, skuId);
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
     * 确保新的 SKU 选择的规格值组合不与同 SPU 下的其他 SKU 重复
     *
     * @param productId           产品 ID
     * @param newSpecRelationList 新规格绑定列表
     */
    private void ensureSkuSpecRelationValidate(@NotNull Long productId, @NotNull List<SkuSpecRelation> newSpecRelationList) {
        Product product = ensureProduct(productId);
        List<Sku> skuList = skuRepository.listByProductId(productId, null);
        // 获取本 SPU 下所有已存在的 SKU 选择的规格值组合
        List<List<Long>> specValueGroupList = new ArrayList<>();
        for (Sku s : skuList) {
            List<Long> valueList = s.getSpecs().stream()
                    .map(SkuSpecRelation::getValueId)
                    .toList();
            specValueGroupList.add(valueList);
        }
        // 新的 SKU 选择的规格值组合
        List<Long> newSpecRelationValueIdList = newSpecRelationList.stream()
                .map(SkuSpecRelation::getValueId)
                .toList();
        // SPU 下必选的规格 ID 列表
        List<ProductSpec> requiredSpecList = product.getSpecs().stream()
                .filter(ProductSpec::isRequired)
                .toList();
        // 获取 Spec ID -> List< Spec Value > 映射
        Map<Long, ProductSpecValue> specValueByValueIdMap = product.getSpecs().stream()
                .map(ProductSpec::getValues)
                .flatMap(List::stream)
                .collect(Collectors.toMap(ProductSpecValue::getId, Function.identity()));
        // 如果 SPU 必选规格数量大于 SKU 选择规格数量, 则找出第一个缺失的必填规格名称, 抛出异常
        if (requiredSpecList.size() > newSpecRelationValueIdList.size())
            requiredSpecList.stream()
                    .filter(spec ->
                            !newSpecRelationList.stream().map(SkuSpecRelation::getSpecId).toList().contains(spec.getId())
                    )
                    .findFirst()
                    .ifPresent(spec -> {
                        throw new ConflictException("规格 '" + spec.getSpecName() + "' 必填");
                    });
        // 遍历有已存在的 SKU 选择的规格值组合, 如果有与新的选择的规格值组合相同的, 则获取这些组合的名称, 然后抛出异常
        for (List<Long> specValueIdList : specValueGroupList)
            if (specValueIdList.equals(newSpecRelationValueIdList)) {
                String existingSpecValueMsg = specValueIdList.stream()
                        .map(specValueByValueIdMap::get)
                        .map(ProductSpecValue::getValueName)
                        .collect(Collectors.joining(", "));
                throw new ConflictException("规格组合: [ " + existingSpecValueMsg + " ] 已被占用");
            }
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
