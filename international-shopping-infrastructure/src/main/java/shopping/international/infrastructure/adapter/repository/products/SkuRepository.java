package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.ISkuRepository;
import shopping.international.domain.model.aggregate.products.Sku;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPrice;
import shopping.international.domain.model.vo.products.SkuSpecRelation;
import shopping.international.infrastructure.dao.products.*;
import shopping.international.infrastructure.dao.products.po.*;
import shopping.international.types.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus 的 SKU 聚合仓储实现
 *
 * <p>负责组合加载 SKU 及其价格, 规格绑定与图库, 并处理默认 SKU 与库存的持久化逻辑</p>
 */
@Repository
@RequiredArgsConstructor
public class SkuRepository implements ISkuRepository {

    /**
     * SKU 主表 Mapper
     */
    private final ProductSkuMapper productSkuMapper;
    /**
     * SKU 价格 Mapper
     */
    private final ProductPriceMapper productPriceMapper;
    /**
     * SKU 规格绑定 Mapper
     */
    private final ProductSkuSpecMapper productSkuSpecMapper;
    /**
     * SKU 图片 Mapper
     */
    private final ProductSkuImageMapper productSkuImageMapper;
    /**
     * 规格 Mapper, 用于规格绑定冗余名称
     */
    private final ProductSpecMapper productSpecMapper;
    /**
     * 规格值 Mapper, 用于规格绑定冗余名称
     */
    private final ProductSpecValueMapper productSpecValueMapper;
    /**
     * 商品 Mapper, 用于默认 SKU 与库存更新
     */
    private final ProductMapper productMapper;

    /**
     * 按主键查询 SKU 聚合
     *
     * @param productId 所属商品 ID
     * @param skuId     SKU ID
     * @return 聚合快照, 不存在则返回空
     */
    @Override
    public @NotNull Optional<Sku> findById(@NotNull Long productId, @NotNull Long skuId) {
        ProductSkuPO po = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getId, skuId)
                .eq(ProductSkuPO::getProductId, productId)
                .last("limit 1"));
        if (po == null)
            return Optional.empty();
        return Optional.of(buildAggregate(List.of(po)).get(0));
    }

    /**
     * 按商品查询 SKU 列表
     *
     * @param productId 所属商品 ID
     * @param status    状态过滤, 为空则不过滤
     * @return SKU 聚合列表
     */
    @Override
    public @NotNull List<Sku> listByProductId(@NotNull Long productId, @Nullable SkuStatus status) {
        LambdaQueryWrapper<ProductSkuPO> wrapper = new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getProductId, productId);
        if (status != null)
            wrapper.eq(ProductSkuPO::getStatus, status.name());
        wrapper.orderByAsc(ProductSkuPO::getId);
        List<ProductSkuPO> pos = productSkuMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return buildAggregate(pos);
    }

    /**
     * 新增 SKU 聚合
     *
     * @param sku 待保存的聚合, ID 为空
     * @return 携带持久化 ID 的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Sku save(@NotNull Sku sku) {
        ProductSkuPO po = ProductSkuPO.builder()
                .productId(sku.getProductId())
                .skuCode(sku.getSkuCode())
                .stock(sku.getStock())
                .weight(sku.getWeight())
                .status(sku.getStatus().name())
                .isDefault(sku.isDefaultSku())
                .barcode(sku.getBarcode())
                .createdAt(sku.getCreatedAt())
                .updatedAt(sku.getUpdatedAt())
                .build();
        try {
            productSkuMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("SKU 唯一约束冲突", e);
        }
        sku.assignId(po.getId());
        persistPrices(po.getId(), sku.getPrices());
        persistSpecs(po.getId(), sku.getSpecs());
        persistImages(po.getId(), sku.getImages());
        return findById(sku.getProductId(), po.getId()).orElseThrow(() -> new ConflictException("SKU 创建后回读失败"));
    }

    /**
     * 更新 SKU 基础字段及可选图库
     *
     * @param sku           聚合快照
     * @param replaceImages 是否替换图库
     * @return 更新后的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Sku updateBasic(@NotNull Sku sku, boolean replaceImages) {
        LambdaUpdateWrapper<ProductSkuPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductSkuPO::getId, sku.getId())
                .set(ProductSkuPO::getSkuCode, sku.getSkuCode())
                .set(ProductSkuPO::getStock, sku.getStock())
                .set(ProductSkuPO::getWeight, sku.getWeight())
                .set(ProductSkuPO::getStatus, sku.getStatus().name())
                .set(ProductSkuPO::getIsDefault, sku.isDefaultSku())
                .set(ProductSkuPO::getBarcode, sku.getBarcode())
                .set(ProductSkuPO::getUpdatedAt, LocalDateTime.now());
        try {
            productSkuMapper.update(null, wrapper);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("SKU 唯一约束冲突", e);
        }
        if (replaceImages) {
            productSkuImageMapper.delete(new LambdaQueryWrapper<ProductSkuImagePO>()
                    .eq(ProductSkuImagePO::getSkuId, sku.getId()));
            persistImages(sku.getId(), sku.getImages());
        }
        return findById(sku.getProductId(), sku.getId())
                .orElseThrow(() -> new ConflictException("SKU 更新后回读失败"));
    }

    /**
     * 按规格进行增量 upsert
     *
     * @param skuId     SKU ID
     * @param relations 规格绑定列表
     * @return 受影响的规格 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull List<Long> upsertSpecs(@NotNull Long skuId, @NotNull List<SkuSpecRelation> relations) {
        List<Long> affected = new ArrayList<>();
        for (SkuSpecRelation relation : relations) {
            LambdaUpdateWrapper<ProductSkuSpecPO> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(ProductSkuSpecPO::getSkuId, skuId)
                    .eq(ProductSkuSpecPO::getSpecId, relation.getSpecId())
                    .set(ProductSkuSpecPO::getValueId, relation.getValueId());
            int updated = productSkuSpecMapper.update(null, wrapper);
            if (updated == 0) {
                ProductSkuSpecPO po = ProductSkuSpecPO.builder()
                        .skuId(skuId)
                        .specId(relation.getSpecId())
                        .valueId(relation.getValueId())
                        .createdAt(LocalDateTime.now())
                        .build();
                productSkuSpecMapper.insert(po);
            }
            affected.add(relation.getSpecId());
        }
        return affected;
    }

    /**
     * 删除指定规格绑定
     *
     * @param skuId  SKU ID
     * @param specId 规格 ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpec(@NotNull Long skuId, @NotNull Long specId) {
        int deleted = productSkuSpecMapper.delete(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .eq(ProductSkuSpecPO::getSkuId, skuId)
                .eq(ProductSkuSpecPO::getSpecId, specId));
        return deleted > 0;
    }

    /**
     * 按币种增量 upsert 价格
     *
     * @param skuId  SKU ID
     * @param prices 价格列表
     * @return 受影响的币种列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull List<String> upsertPrices(@NotNull Long skuId, @NotNull List<ProductPrice> prices) {
        List<ProductPricePO> existing = productPriceMapper.selectList(new LambdaQueryWrapper<ProductPricePO>()
                .eq(ProductPricePO::getSkuId, skuId));
        Map<String, ProductPricePO> existingMap = existing == null ? Collections.emptyMap()
                : existing.stream().collect(Collectors.toMap(ProductPricePO::getCurrency, po -> po));
        LocalDateTime now = LocalDateTime.now();
        List<String> currencies = new ArrayList<>();
        for (ProductPrice price : prices) {
            ProductPricePO found = existingMap.get(price.getCurrency());
            if (found == null) {
                ProductPricePO po = ProductPricePO.builder()
                        .skuId(skuId)
                        .currency(price.getCurrency())
                        .listPrice(price.getListPrice())
                        .salePrice(price.getSalePrice())
                        .isActive(price.isActive())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                productPriceMapper.insert(po);
            } else {
                LambdaUpdateWrapper<ProductPricePO> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(ProductPricePO::getSkuId, skuId)
                        .eq(ProductPricePO::getCurrency, price.getCurrency())
                        .set(ProductPricePO::getListPrice, price.getListPrice())
                        .set(ProductPricePO::getSalePrice, price.getSalePrice())
                        .set(ProductPricePO::getIsActive, price.isActive())
                        .set(ProductPricePO::getUpdatedAt, now);
                productPriceMapper.update(null, wrapper);
            }
            currencies.add(price.getCurrency());
        }
        return currencies;
    }

    /**
     * 覆盖更新库存
     *
     * @param skuId SKU ID
     * @param stock 新库存
     * @return 更新后的库存值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStock(@NotNull Long skuId, int stock) {
        LambdaUpdateWrapper<ProductSkuPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductSkuPO::getId, skuId)
                .set(ProductSkuPO::getStock, stock)
                .set(ProductSkuPO::getUpdatedAt, LocalDateTime.now());
        productSkuMapper.update(null, wrapper);
        return stock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDefault(@NotNull Long productId, @Nullable Long skuId) {
        LambdaUpdateWrapper<ProductSkuPO> clearWrapper = new LambdaUpdateWrapper<>();
        clearWrapper.eq(ProductSkuPO::getProductId, productId)
                .set(ProductSkuPO::getIsDefault, false)
                .set(ProductSkuPO::getUpdatedAt, LocalDateTime.now());
        productSkuMapper.update(null, clearWrapper);
        if (skuId != null) {
            LambdaUpdateWrapper<ProductSkuPO> setWrapper = new LambdaUpdateWrapper<>();
            setWrapper.eq(ProductSkuPO::getId, skuId)
                    .set(ProductSkuPO::getIsDefault, true)
                    .set(ProductSkuPO::getUpdatedAt, LocalDateTime.now());
            productSkuMapper.update(null, setWrapper);
        }
        LambdaUpdateWrapper<ProductPO> productWrapper = new LambdaUpdateWrapper<>();
        productWrapper.eq(ProductPO::getId, productId)
                .set(ProductPO::getDefaultSkuId, skuId)
                .set(ProductPO::getUpdatedAt, LocalDateTime.now());
        productMapper.update(null, productWrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int sumStockByProduct(@NotNull Long productId) {
        List<ProductSkuPO> pos = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getProductId, productId));
        if (pos == null || pos.isEmpty())
            return 0;
        return pos.stream()
                .map(ProductSkuPO::getStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * 组合构建 SKU 聚合
     *
     * @param skuPos SKU 持久化对象列表
     * @return SKU 聚合列表
     */
    private List<Sku> buildAggregate(@NotNull List<ProductSkuPO> skuPos) {
        List<Long> skuIds = skuPos.stream().map(ProductSkuPO::getId).toList();
        Map<Long, List<ProductPrice>> priceMap = loadPrices(skuIds);
        Map<Long, List<SkuSpecRelation>> specMap = loadSpecRelations(skuIds);
        Map<Long, List<ProductImage>> imageMap = loadImages(skuIds);
        List<Sku> list = new ArrayList<>();
        for (ProductSkuPO po : skuPos) {
            Sku sku = Sku.reconstitute(
                    po.getId(), po.getProductId(), po.getSkuCode(),
                    po.getStock() == null ? 0 : po.getStock(),
                    po.getWeight(),
                    SkuStatus.from(po.getStatus()),
                    Boolean.TRUE.equals(po.getIsDefault()),
                    po.getBarcode(),
                    priceMap.getOrDefault(po.getId(), Collections.emptyList()),
                    specMap.getOrDefault(po.getId(), Collections.emptyList()),
                    imageMap.getOrDefault(po.getId(), Collections.emptyList()),
                    po.getCreatedAt(), po.getUpdatedAt()
            );
            list.add(sku);
        }
        return list;
    }

    /**
     * 加载 SKU 价格
     *
     * @param skuIds SKU ID 列表
     * @return SKU ID -> 价格列表
     */
    private Map<Long, List<ProductPrice>> loadPrices(@NotNull List<Long> skuIds) {
        if (skuIds.isEmpty())
            return Collections.emptyMap();
        List<ProductPricePO> pos = productPriceMapper.selectList(new LambdaQueryWrapper<ProductPricePO>()
                .in(ProductPricePO::getSkuId, skuIds));
        Map<Long, List<ProductPrice>> map = new HashMap<>();
        for (ProductPricePO po : pos) {
            ProductPrice price = ProductPrice.of(po.getCurrency(), po.getListPrice(), po.getSalePrice(),
                    Boolean.TRUE.equals(po.getIsActive()));
            map.computeIfAbsent(po.getSkuId(), k -> new ArrayList<>()).add(price);
        }
        return map;
    }

    /**
     * 加载 SKU 规格绑定
     *
     * @param skuIds SKU ID 列表
     * @return SKU ID -> 规格绑定列表
     */
    private Map<Long, List<SkuSpecRelation>> loadSpecRelations(@NotNull List<Long> skuIds) {
        if (skuIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSkuSpecPO> pos = productSkuSpecMapper.selectList(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .in(ProductSkuSpecPO::getSkuId, skuIds));
        if (pos == null || pos.isEmpty())
            return Collections.emptyMap();
        Set<Long> specIds = pos.stream().map(ProductSkuSpecPO::getSpecId).collect(Collectors.toSet());
        Set<Long> valueIds = pos.stream().map(ProductSkuSpecPO::getValueId).collect(Collectors.toSet());
        Map<Long, ProductSpecPO> specMap = loadSpecSnapshot(specIds);
        Map<Long, ProductSpecValuePO> valueMap = loadSpecValueSnapshot(valueIds);
        Map<Long, List<SkuSpecRelation>> map = new HashMap<>();
        for (ProductSkuSpecPO po : pos) {
            ProductSpecPO spec = specMap.get(po.getSpecId());
            ProductSpecValuePO value = valueMap.get(po.getValueId());
            String specCode = spec == null ? null : spec.getSpecCode();
            String specName = spec == null ? null : spec.getSpecName();
            String valueCode = value == null ? null : value.getValueCode();
            String valueName = value == null ? null : value.getValueName();
            SkuSpecRelation relation = SkuSpecRelation.of(po.getSpecId(), specCode, specName,
                    po.getValueId(), valueCode, valueName);
            map.computeIfAbsent(po.getSkuId(), k -> new ArrayList<>()).add(relation);
        }
        return map;
    }

    /**
     * 加载规格快照
     *
     * @param specIds 规格 ID 集合
     * @return 规格 ID -> 规格快照
     */
    private Map<Long, ProductSpecPO> loadSpecSnapshot(@NotNull Set<Long> specIds) {
        if (specIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSpecPO> specPos = productSpecMapper.selectByIds(specIds);
        if (specPos == null)
            return Collections.emptyMap();
        return specPos.stream().collect(Collectors.toMap(ProductSpecPO::getId, po -> po));
    }

    /**
     * 加载规格值快照
     *
     * @param valueIds 规格值 ID 集合
     * @return 规格值 ID -> 规格值快照
     */
    private Map<Long, ProductSpecValuePO> loadSpecValueSnapshot(@NotNull Set<Long> valueIds) {
        if (valueIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSpecValuePO> valuePos = productSpecValueMapper.selectByIds(valueIds);
        if (valuePos == null)
            return Collections.emptyMap();
        return valuePos.stream().collect(Collectors.toMap(ProductSpecValuePO::getId, po -> po));
    }

    /**
     * 加载 SKU 图库
     *
     * @param skuIds SKU ID 列表
     * @return SKU ID -> 图库
     */
    private Map<Long, List<ProductImage>> loadImages(@NotNull List<Long> skuIds) {
        if (skuIds.isEmpty())
            return Collections.emptyMap();
        List<ProductSkuImagePO> pos = productSkuImageMapper.selectList(new LambdaQueryWrapper<ProductSkuImagePO>()
                .in(ProductSkuImagePO::getSkuId, skuIds)
                .orderByDesc(ProductSkuImagePO::getIsMain)
                .orderByAsc(ProductSkuImagePO::getSortOrder)
                .orderByAsc(ProductSkuImagePO::getId));
        Map<Long, List<ProductImage>> map = new HashMap<>();
        for (ProductSkuImagePO po : pos) {
            ProductImage image = ProductImage.of(
                    po.getUrl(),
                    Boolean.TRUE.equals(po.getIsMain()),
                    po.getSortOrder() == null ? 0 : po.getSortOrder()
            );
            map.computeIfAbsent(po.getSkuId(), k -> new ArrayList<>()).add(image);
        }
        return map;
    }

    /**
     * 持久化价格列表
     *
     * @param skuId  SKU ID
     * @param prices 价格列表
     */
    private void persistPrices(@NotNull Long skuId, @NotNull List<ProductPrice> prices) {
        if (prices.isEmpty())
            return;
        LocalDateTime now = LocalDateTime.now();
        for (ProductPrice price : prices) {
            ProductPricePO po = ProductPricePO.builder()
                    .skuId(skuId)
                    .currency(price.getCurrency())
                    .listPrice(price.getListPrice())
                    .salePrice(price.getSalePrice())
                    .isActive(price.isActive())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            productPriceMapper.insert(po);
        }
    }

    /**
     * 持久化规格绑定
     *
     * @param skuId SKU ID
     * @param specs 规格绑定
     */
    private void persistSpecs(@NotNull Long skuId, @NotNull List<SkuSpecRelation> specs) {
        if (specs.isEmpty())
            return;
        LocalDateTime now = LocalDateTime.now();
        for (SkuSpecRelation relation : specs) {
            ProductSkuSpecPO po = ProductSkuSpecPO.builder()
                    .skuId(skuId)
                    .specId(relation.getSpecId())
                    .valueId(relation.getValueId())
                    .createdAt(now)
                    .build();
            productSkuSpecMapper.insert(po);
        }
    }

    /**
     * 持久化图库
     *
     * @param skuId  SKU ID
     * @param images 图库列表
     */
    private void persistImages(@NotNull Long skuId, @NotNull List<ProductImage> images) {
        if (images.isEmpty())
            return;
        LocalDateTime now = LocalDateTime.now();
        for (ProductImage image : images) {
            ProductSkuImagePO po = ProductSkuImagePO.builder()
                    .skuId(skuId)
                    .url(image.getUrl())
                    .isMain(image.isMain())
                    .sortOrder(image.getSortOrder())
                    .createdAt(now)
                    .build();
            productSkuImageMapper.insert(po);
        }
    }
}
