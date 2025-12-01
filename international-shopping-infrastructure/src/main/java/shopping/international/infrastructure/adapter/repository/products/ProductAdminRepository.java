package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductAdminRepository;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.*;
import shopping.international.infrastructure.dao.products.*;
import shopping.international.infrastructure.dao.products.po.*;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品管理仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductAdminRepository implements IProductAdminRepository {
    /**
     * JSON 序列化工具
     */
    private final ObjectMapper objectMapper;
    /**
     * 商品 Mapper
     */
    private final ProductMapper productMapper;
    /**
     * 商品 i18n Mapper
     */
    private final ProductI18nMapper productI18nMapper;
    /**
     * 商品图片 Mapper
     */
    private final ProductImageMapper productImageMapper;
    /**
     * 规格 Mapper
     */
    private final ProductSpecMapper productSpecMapper;
    /**
     * 规格 i18n Mapper
     */
    private final ProductSpecI18nMapper productSpecI18nMapper;
    /**
     * 规格值 Mapper
     */
    private final ProductSpecValueMapper productSpecValueMapper;
    /**
     * 规格值 i18n Mapper
     */
    private final ProductSpecValueI18nMapper productSpecValueI18nMapper;
    /**
     * SKU Mapper
     */
    private final ProductSkuMapper productSkuMapper;
    /**
     * SKU 图片 Mapper
     */
    private final ProductSkuImageMapper productSkuImageMapper;
    /**
     * 价格 Mapper
     */
    private final ProductPriceMapper productPriceMapper;
    /**
     * SKU 规格绑定 Mapper
     */
    private final ProductSkuSpecMapper productSkuSpecMapper;

    /**
     * 分页查询商品
     *
     * @param page           页码
     * @param size           每页大小
     * @param status         状态过滤
     * @param skuType        SKU 类型
     * @param categoryId     分类 ID
     * @param keyword        关键词
     * @param tag            标签
     * @param includeDeleted 是否包含已删除
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<Product> page(int page, int size, String status, String skuType, Long categoryId, String keyword, String tag, boolean includeDeleted) {
        long total = productMapper.countAdminPage(status, skuType, categoryId, keyword, tag, includeDeleted);
        if (total == 0)
            return new PageResult<>(List.of(), 0);
        int offset = Math.max(page - 1, 0) * size;
        List<ProductPO> records = productMapper.selectAdminPage(status, skuType, categoryId, keyword, tag, includeDeleted, size, offset);
        List<Product> items = records.stream().map(this::toEntity).toList();
        return new PageResult<>(items, total);
    }

    /**
     * 按 ID 查询商品
     *
     * @param productId 商品 ID
     * @return 商品
     */
    @Override
    public @NotNull Optional<Product> findById(@NotNull Long productId) {
        ProductPO po = productMapper.selectById(productId);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public @NotNull Optional<ProductSpec> findSpecById(@NotNull Long specId) {
        ProductSpecPO po = productSpecMapper.selectById(specId);
        return Optional.ofNullable(po).map(this::toProductSpec);
    }

    @Override
    public @NotNull Optional<ProductSpecValue> findSpecValueById(@NotNull Long valueId) {
        ProductSpecValuePO po = productSpecValueMapper.selectById(valueId);
        return Optional.ofNullable(po).map(this::toProductSpecValue);
    }

    /**
     * 批量查询商品
     *
     * @param productIds 商品 ID 集合
     * @return 商品映射
     */
    @Override
    public @NotNull Map<Long, Product> mapByIds(@NotNull Set<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductPO> records = productMapper.selectByIds(productIds);
        return records.stream()
                .map(this::toEntity)
                .collect(Collectors.toMap(Product::getId, Function.identity(),
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    /**
     * 批量查询商品多语言
     *
     * @param productIds 商品 ID 集合
     * @return productId -> 多语言列表
     */
    @Override
    public @NotNull Map<Long, List<ProductI18n>> mapI18n(@NotNull Collection<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductI18nPO> records = productI18nMapper.selectList(new LambdaQueryWrapper<ProductI18nPO>()
                .in(ProductI18nPO::getProductId, productIds)
                .orderByAsc(ProductI18nPO::getProductId, ProductI18nPO::getLocale));
        Map<Long, List<ProductI18n>> result = new LinkedHashMap<>();
        for (ProductI18nPO po : records) {
            result.computeIfAbsent(po.getProductId(), ignore -> new ArrayList<>())
                    .add(toProductI18n(po));
        }
        return result;
    }

    /**
     * 获取商品图库
     *
     * @param productId 商品 ID
     * @return 图库列表
     */
    @Override
    public @NotNull List<ProductImage> listGallery(@NotNull Long productId) {
        List<ProductImagePO> records = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .eq(ProductImagePO::getProductId, productId)
                .orderByAsc(ProductImagePO::getSortOrder, ProductImagePO::getId));
        return records.stream().map(this::toProductImage).toList();
    }

    /**
     * 批量获取商品图库
     *
     * @param productIds 商品 ID
     * @return productId -> 图库
     */
    @Override
    public @NotNull Map<Long, List<ProductImage>> mapGallery(@NotNull Collection<Long> productIds) {
        if (productIds.isEmpty())
            return Map.of();
        List<ProductImagePO> records = productImageMapper.selectList(new LambdaQueryWrapper<ProductImagePO>()
                .in(ProductImagePO::getProductId, productIds)
                .orderByAsc(ProductImagePO::getSortOrder, ProductImagePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(ProductImagePO::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductImage, Collectors.toList())));
    }

    /**
     * 新增商品
     *
     * @param product 商品实体
     * @return 生成的主键 ID
     */
    @Override
    public @NotNull Long insert(@NotNull Product product) {
        ProductPO po = toPo(product);
        productMapper.insert(po);
        return po.getId();
    }

    /**
     * 更新商品
     *
     * @param product 商品实体
     */
    @Override
    public void update(@NotNull Product product) {
        productMapper.updateById(toPo(product));
    }

    /**
     * 批量 upsert 商品多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertI18n(@NotNull Long productId, @NotNull List<ProductI18n> payloads) {
        if (payloads.isEmpty())
            return;
        // 根据 productId 获取 locale -> i18n PO 映射
        Map<String, ProductI18nPO> existingMap = productI18nMapper.selectList(new LambdaQueryWrapper<ProductI18nPO>()
                        .eq(ProductI18nPO::getProductId, productId))
                .stream()
                .collect(Collectors.toMap(ProductI18nPO::getLocale, Function.identity(),
                        (current, ignore) -> current, LinkedHashMap::new));
        for (ProductI18n vo : payloads) {
            ProductI18nPO po = existingMap.get(vo.getLocale());
            if (po == null) {
                productI18nMapper.insert(ProductI18nPO.builder()
                        .productId(productId)
                        .locale(vo.getLocale())
                        .title(vo.getTitle())
                        .subtitle(vo.getSubtitle())
                        .description(vo.getDescription())
                        .slug(vo.getSlug())
                        .tags(toJson(vo.getTags()))
                        .build());
                continue;
            }
            po.setTitle(vo.getTitle());
            po.setSubtitle(vo.getSubtitle());
            po.setDescription(vo.getDescription());
            po.setSlug(vo.getSlug());
            po.setTags(toJson(vo.getTags()));
            productI18nMapper.updateById(po);
        }
    }

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   图库列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery) {
        productImageMapper.delete(new LambdaQueryWrapper<ProductImagePO>()
                .eq(ProductImagePO::getProductId, productId));
        if (gallery.isEmpty())
            return;
        for (ProductImage image : gallery) {
            productImageMapper.insert(ProductImagePO.builder()
                    .productId(productId)
                    .url(image.getUrl())
                    .isMain(image.isMain() ? 1 : 0)
                    .sortOrder(image.getSortOrder())
                    .build());
        }
    }

    /**
     * 判断基础 slug 是否重复
     *
     * @param slug      slug
     * @param excludeId 排除 ID
     * @return 是否存在
     */
    @Override
    public boolean existsSlug(@NotNull String slug, Long excludeId) {
        return productMapper.selectCount(new LambdaQueryWrapper<ProductPO>()
                .eq(ProductPO::getSlug, slug)
                .ne(excludeId != null, ProductPO::getId, excludeId)) > 0;
    }

    /**
     * 判断 locale + slug 是否冲突
     *
     * @param locale           语言
     * @param slug             slug
     * @param excludeProductId 排除商品 ID
     * @return 是否存在
     */
    @Override
    public boolean existsLocalizedSlug(@NotNull String locale, @NotNull String slug, Long excludeProductId) {
        return productI18nMapper.selectCount(new LambdaQueryWrapper<ProductI18nPO>()
                .eq(ProductI18nPO::getLocale, locale)
                .eq(ProductI18nPO::getSlug, slug)
                .ne(excludeProductId != null, ProductI18nPO::getProductId, excludeProductId)) > 0;
    }

    /**
     * 查询规格
     *
     * @param productId       商品 ID
     * @param includeDisabled 是否包含禁用项
     * @return 规格列表
     */
    @Override
    public @NotNull List<ProductSpec> listSpecs(@NotNull Long productId, boolean includeDisabled) {
        List<ProductSpecPO> records = productSpecMapper.selectList(new LambdaQueryWrapper<ProductSpecPO>()
                .eq(ProductSpecPO::getProductId, productId)
                .ne(!includeDisabled, ProductSpecPO::getStatus, SkuStatus.DISABLED.name())
                .orderByAsc(ProductSpecPO::getSortOrder, ProductSpecPO::getId));
        return records.stream().map(this::toProductSpec).toList();
    }

    /**
     * 查询规格值
     *
     * @param productId       商品 ID
     * @param includeDisabled 是否包含禁用项
     * @return 规格值列表
     */
    @Override
    public @NotNull List<ProductSpecValue> listSpecValues(@NotNull Long productId, boolean includeDisabled) {
        List<ProductSpecValuePO> records = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getProductId, productId)
                .ne(!includeDisabled, ProductSpecValuePO::getStatus, SkuStatus.DISABLED.name())
                .orderByAsc(ProductSpecValuePO::getSortOrder, ProductSpecValuePO::getId));
        return records.stream().map(this::toProductSpecValue).toList();
    }

    @Override
    public @NotNull List<ProductSpecValue> listSpecValues(@NotNull Long productId, @NotNull Long specId, boolean includeDisabled) {
        List<ProductSpecValuePO> records = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getProductId, productId)
                .eq(ProductSpecValuePO::getSpecId, specId)
                .ne(!includeDisabled, ProductSpecValuePO::getStatus, SkuStatus.DISABLED.name())
                .orderByAsc(ProductSpecValuePO::getSortOrder, ProductSpecValuePO::getId));
        return records.stream().map(this::toProductSpecValue).toList();
    }

    /**
     * 查询规格 I18N
     *
     * @param specIds 规格 ID 集合
     * @return 映射
     */
    @Override
    public @NotNull Map<Long, List<ProductSpecI18n>> mapSpecI18n(@NotNull Set<Long> specIds) {
        if (specIds.isEmpty())
            return Map.of();
        List<ProductSpecI18nPO> records = productSpecI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecI18nPO>()
                .in(ProductSpecI18nPO::getSpecId, specIds)
                .orderByAsc(ProductSpecI18nPO::getLocale));
        Map<Long, List<ProductSpecI18n>> result = new LinkedHashMap<>();
        for (ProductSpecI18nPO po : records) {
            result.computeIfAbsent(po.getSpecId(), ignore -> new ArrayList<>())
                    .add(ProductSpecI18n.of(po.getLocale(), po.getSpecName()));
        }
        return result;
    }

    /**
     * 查询规格值 I18N
     *
     * @param valueIds 规格值 ID 集合
     * @return 映射
     */
    @Override
    public @NotNull Map<Long, List<ProductSpecValueI18n>> mapSpecValueI18n(@NotNull Set<Long> valueIds) {
        if (valueIds.isEmpty())
            return Map.of();
        List<ProductSpecValueI18nPO> records = productSpecValueI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                .in(ProductSpecValueI18nPO::getValueId, valueIds)
                .orderByAsc(ProductSpecValueI18nPO::getLocale));
        Map<Long, List<ProductSpecValueI18n>> result = new LinkedHashMap<>();
        for (ProductSpecValueI18nPO po : records) {
            result.computeIfAbsent(po.getValueId(), ignore -> new ArrayList<>())
                    .add(ProductSpecValueI18n.of(po.getLocale(), po.getValueName()));
        }
        return result;
    }

    /**
     * 增量维护规格
     *
     * @param productId   商品 ID
     * @param commandList 规格命令
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertSpecs(@NotNull Long productId, @NotNull List<ProductSpecUpsertCommand> commandList) {
        if (commandList.isEmpty())
            return;
        for (ProductSpecUpsertCommand command : commandList) {
            Long specId = command.getSpecId();
            if (specId == null) {
                ProductSpecPO po = ProductSpecPO.builder()
                        .productId(productId)
                        .specCode(command.getSpecCode())
                        .specName(command.getSpecName())
                        .specType(command.getSpecType() == null ? SpecType.OTHER.name() : command.getSpecType().name())
                        .isRequired(command.isRequired() ? 1 : 0)
                        .sortOrder(command.getSortOrder())
                        .status(command.isEnabled() ? SkuStatus.ENABLED.name() : SkuStatus.DISABLED.name())
                        .build();
                productSpecMapper.insert(po);
                specId = po.getId();
            } else {
                ProductSpecPO po = ProductSpecPO.builder()
                        .id(specId)
                        .productId(productId)
                        .specCode(command.getSpecCode())
                        .specName(command.getSpecName())
                        .specType(command.getSpecType() == null ? SpecType.OTHER.name() : command.getSpecType().name())
                        .isRequired(command.isRequired() ? 1 : 0)
                        .sortOrder(command.getSortOrder())
                        .status(command.isEnabled() ? SkuStatus.ENABLED.name() : SkuStatus.DISABLED.name())
                        .build();
                productSpecMapper.updateById(po);
            }
            upsertSpecI18n(specId, command.getI18nList());
            if (command.getValues() == null)
                continue;
            for (ProductSpecValueUpsertCommand valueCommand : command.getValues()) {
                Long valueId = valueCommand.getValueId();
                if (valueId == null) {
                    ProductSpecValuePO valuePo = ProductSpecValuePO.builder()
                            .productId(productId)
                            .specId(specId)
                            .valueCode(valueCommand.getValueCode())
                            .valueName(valueCommand.getValueName())
                            .attributes(toJson(valueCommand.getAttributes()))
                            .sortOrder(valueCommand.getSortOrder())
                            .status(valueCommand.isEnabled() ? SkuStatus.ENABLED.name() : SkuStatus.DISABLED.name())
                            .build();
                    productSpecValueMapper.insert(valuePo);
                    valueId = valuePo.getId();
                } else {
                    ProductSpecValuePO valuePo = ProductSpecValuePO.builder()
                            .id(valueId)
                            .productId(productId)
                            .specId(specId)
                            .valueCode(valueCommand.getValueCode())
                            .valueName(valueCommand.getValueName())
                            .attributes(toJson(valueCommand.getAttributes()))
                            .sortOrder(valueCommand.getSortOrder())
                            .status(valueCommand.isEnabled() ? SkuStatus.ENABLED.name() : SkuStatus.DISABLED.name())
                            .build();
                    productSpecValueMapper.updateById(valuePo);
                }
                upsertSpecValueI18n(valueId, valueCommand.getI18nList());
            }
        }
    }

    /**
     * 查询 SKU
     *
     * @param productId 商品 ID
     * @return SKU 列表
     */
    @Override
    public @NotNull List<ProductSku> listSkus(@NotNull Long productId) {
        List<ProductSkuPO> records = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getProductId, productId)
                .orderByDesc(ProductSkuPO::getIsDefault)
                .orderByAsc(ProductSkuPO::getId));
        return records.stream().map(this::toProductSku).toList();
    }

    /**
     * 查询 SKU 价格
     *
     * @param skuIds SKU ID 集合
     * @return 映射
     */
    @Override
    public @NotNull Map<Long, List<ProductPrice>> mapActivePrices(@NotNull Set<Long> skuIds) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductPricePO> records = productPriceMapper.selectList(new LambdaQueryWrapper<ProductPricePO>()
                .in(ProductPricePO::getSkuId, skuIds)
                .eq(ProductPricePO::getIsActive, 1)
                .orderByDesc(ProductPricePO::getUpdatedAt, ProductPricePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(ProductPricePO::getSkuId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductPrice, Collectors.toList())));
    }

    /**
     * 查询 SKU 图片
     *
     * @param skuIds SKU ID 集合
     * @return 映射
     */
    @Override
    public @NotNull Map<Long, List<ProductImage>> mapSkuImages(@NotNull Set<Long> skuIds) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductSkuImagePO> records = productSkuImageMapper.selectList(new LambdaQueryWrapper<ProductSkuImagePO>()
                .in(ProductSkuImagePO::getSkuId, skuIds)
                .orderByAsc(ProductSkuImagePO::getSortOrder, ProductSkuImagePO::getId));
        return records.stream()
                .collect(Collectors.groupingBy(ProductSkuImagePO::getSkuId,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toProductImage, Collectors.toList())));
    }

    /**
     * 查询 SKU 规格绑定
     *
     * @param skuIds SKU ID 集合
     * @return 映射
     */
    @Override
    public @NotNull Map<Long, List<ProductSkuSpec>> mapSkuSpecs(@NotNull Set<Long> skuIds) {
        if (skuIds.isEmpty())
            return Map.of();
        List<ProductSkuSpecPO> records = productSkuSpecMapper.selectList(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .in(ProductSkuSpecPO::getSkuId, skuIds));
        if (records.isEmpty())
            return Map.of();
        Set<Long> specIds = records.stream().map(ProductSkuSpecPO::getSpecId).collect(Collectors.toSet());
        Set<Long> valueIds = records.stream().map(ProductSkuSpecPO::getValueId).collect(Collectors.toSet());
        Map<Long, ProductSpecPO> specMap = productSpecMapper.selectList(new LambdaQueryWrapper<ProductSpecPO>()
                        .in(ProductSpecPO::getId, specIds))
                .stream()
                .collect(Collectors.toMap(ProductSpecPO::getId, Function.identity()));
        Map<Long, ProductSpecValuePO> valueMap = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                        .in(ProductSpecValuePO::getId, valueIds))
                .stream()
                .collect(Collectors.toMap(ProductSpecValuePO::getId, Function.identity()));

        Map<Long, List<ProductSkuSpec>> result = new LinkedHashMap<>();
        for (ProductSkuSpecPO po : records) {
            ProductSpecPO spec = specMap.get(po.getSpecId());
            ProductSpecValuePO value = valueMap.get(po.getValueId());
            if (spec == null || value == null)
                continue;
            ProductSkuSpec skuSpec = ProductSkuSpec.of(spec.getId(), spec.getSpecCode(), spec.getSpecName(),
                    value.getId(), value.getValueCode(), value.getValueName());
            result.computeIfAbsent(po.getSkuId(), ignore -> new ArrayList<>()).add(skuSpec);
        }
        return result;
    }

    /**
     * 创建 SKU
     *
     * @param productId 商品 ID
     * @param commandList  SKU 命令
     * @return SKU 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull List<ProductSku> createSkus(@NotNull Long productId, @NotNull List<ProductSkuUpsertItemCommand> commandList) {
        List<ProductSku> result = new ArrayList<>();
        for (ProductSkuUpsertItemCommand command : commandList) {
            ProductSkuPO po = ProductSkuPO.builder()
                    .productId(productId)
                    .skuCode(command.getSkuCode())
                    .stock(command.getStock())
                    .weight(command.getWeight())
                    .status(command.getStatus() == null ? null : command.getStatus().name())
                    .isDefault(command.isDefault() ? 1 : 0)
                    .barcode(command.getBarcode())
                    .build();
            productSkuMapper.insert(po);
            Long skuId = po.getId();
            upsertPriceListInternal(skuId, command.getPriceList());
            replaceSkuSpecs(skuId, command.getSpecs());
            replaceSkuImages(skuId, command.getImages());
            result.add(toProductSku(po));
        }
        return result;
    }

    /**
     * 更新 SKU
     *
     * @param productId 商品 ID
     * @param commands  SKU 命令
     * @return SKU 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull List<ProductSku> updateSkus(@NotNull Long productId, @NotNull List<ProductSkuUpsertItemCommand> commands) {
        List<ProductSku> result = new ArrayList<>();
        for (ProductSkuUpsertItemCommand command : commands) {
            ProductSkuPO po = ProductSkuPO.builder()
                    .id(command.getId())
                    .productId(productId)
                    .skuCode(command.getSkuCode())
                    .stock(command.getStock())
                    .weight(command.getWeight())
                    .status(command.getStatus() == null ? null : command.getStatus().name())
                    .isDefault(command.isDefault() ? 1 : 0)
                    .barcode(command.getBarcode())
                    .build();
            productSkuMapper.updateById(po);
            upsertPriceListInternal(command.getId(), command.getPriceList());
            replaceSkuSpecs(command.getId(), command.getSpecs());
            replaceSkuImages(command.getId(), command.getImages());
            result.add(toProductSku(productSkuMapper.selectById(command.getId())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSku patchSku(@NotNull Long productId, @NotNull Long skuId, @NotNull ProductSkuPatchCommand command) {
        ProductSkuPO existing = productSkuMapper.selectById(skuId);
        if (existing == null || !Objects.equals(existing.getProductId(), productId))
            throw new IllegalParamException("SKU 不存在");
        ProductSkuPO toUpdate = ProductSkuPO.builder()
                .id(existing.getId())
                .productId(productId)
                .skuCode(command.getSkuCode() == null ? existing.getSkuCode() : command.getSkuCode())
                .stock(command.getStock() == null ? existing.getStock() : command.getStock())
                .weight(command.getWeight() == null ? existing.getWeight() : command.getWeight())
                .status(command.getStatus() == null ? existing.getStatus() : command.getStatus().name())
                .isDefault(command.getIsDefault() == null ? existing.getIsDefault() : (Boolean.TRUE.equals(command.getIsDefault()) ? 1 : 0))
                .barcode(command.getBarcode() == null ? existing.getBarcode() : command.getBarcode())
                .build();
        productSkuMapper.updateById(toUpdate);
        if (command.getImages() != null)
            replaceSkuImages(skuId, command.getImages());
        return toProductSku(productSkuMapper.selectById(skuId));
    }

    /**
     * 更新 SKU 价格
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @return SKU
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSku upsertPrices(@NotNull Long productId, @NotNull Long skuId, @NotNull List<ProductPriceUpsertCommand> commands) {
        ProductSkuPO sku = productSkuMapper.selectById(skuId);
        if (sku == null || !Objects.equals(sku.getProductId(), productId))
            throw new IllegalParamException("SKU 不存在");
        upsertPriceListInternal(skuId, commands);
        return toProductSku(productSkuMapper.selectById(skuId));
    }

    /**
     * 调整 SKU 库存
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   库存命令
     * @return SKU
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSku adjustStock(@NotNull Long productId, @NotNull Long skuId, @NotNull StockAdjustCommand command) {
        ProductSkuPO sku = productSkuMapper.selectById(skuId);
        if (sku == null || !Objects.equals(sku.getProductId(), productId))
            throw new IllegalParamException("SKU 不存在");
        int current = sku.getStock() == null ? 0 : sku.getStock();
        int target = switch (command.getMode()) {
            case SET -> command.getQuantity();
            case INCREASE -> current + command.getQuantity();
            case DECREASE -> current - command.getQuantity();
        };
        if (target < 0)
            throw new IllegalParamException("库存不足");
        sku.setStock(target);
        productSkuMapper.updateById(sku);
        return toProductSku(sku);
    }

    /**
     * 计算库存总量
     *
     * @param productId 商品 ID
     * @return 库存总量
     */
    @Override
    public int sumStock(@NotNull Long productId) {
        List<ProductSkuPO> records = productSkuMapper.selectList(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getProductId, productId));
        return records.stream()
                .map(ProductSkuPO::getStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * 更新商品库存与默认 SKU
     *
     * @param productId    商品 ID
     * @param stockTotal   库存总量
     * @param defaultSkuId 默认 SKU
     */
    @Override
    public void updateProductStockAndDefault(@NotNull Long productId, int stockTotal, Long defaultSkuId) {
        ProductPO po = new ProductPO();
        po.setId(productId);
        po.setStockTotal(stockTotal);
        po.setDefaultSkuId(defaultSkuId);
        productMapper.updateById(po);
    }

    /**
     * 判断 SKU 编码是否存在
     *
     * @param skuCode      SKU 编码
     * @param excludeSkuId 排除的 SKU
     * @return true 表示存在
     */
    @Override
    public boolean existsSkuCode(@NotNull String skuCode, Long excludeSkuId) {
        return productSkuMapper.selectCount(new LambdaQueryWrapper<ProductSkuPO>()
                .eq(ProductSkuPO::getSkuCode, skuCode)
                .ne(excludeSkuId != null, ProductSkuPO::getId, excludeSkuId)) > 0;
    }

    @Override
    public boolean hasSkuBindingWithSpec(@NotNull Long specId) {
        return productSkuSpecMapper.selectCount(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .eq(ProductSkuSpecPO::getSpecId, specId)) > 0;
    }

    @Override
    public boolean hasSkuBindingWithSpecValue(@NotNull Long valueId) {
        return productSkuSpecMapper.selectCount(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .eq(ProductSkuSpecPO::getValueId, valueId)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpec(@NotNull Long specId) {
        productSpecI18nMapper.delete(new LambdaQueryWrapper<ProductSpecI18nPO>()
                .eq(ProductSpecI18nPO::getSpecId, specId));
        List<ProductSpecValuePO> values = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getSpecId, specId));
        if (!values.isEmpty()) {
            Set<Long> valueIds = values.stream().map(ProductSpecValuePO::getId).collect(Collectors.toSet());
            productSpecValueI18nMapper.delete(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                    .in(ProductSpecValueI18nPO::getValueId, valueIds));
            productSpecValueMapper.deleteBatchIds(valueIds);
        }
        productSpecMapper.deleteById(specId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpecValue(@NotNull Long valueId) {
        productSpecValueI18nMapper.delete(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                .eq(ProductSpecValueI18nPO::getValueId, valueId));
        productSpecValueMapper.deleteById(valueId);
    }

    /**
     * 查询单个 SKU
     *
     * @param skuId SKU ID
     * @return SKU
     */
    @Override
    public @NotNull Optional<ProductSku> findSkuById(@NotNull Long skuId) {
        ProductSkuPO po = productSkuMapper.selectById(skuId);
        return Optional.ofNullable(po).map(this::toProductSku);
    }

    /**
     * 将 ProductPO 转换为领域实体
     *
     * @param po 数据库持久化对象
     * @return 商品实体
     */
    private Product toEntity(ProductPO po) {
        return Product.reconstitute(
                po.getId(),
                po.getSlug(),
                po.getTitle(),
                po.getSubtitle(),
                po.getDescription(),
                po.getCategoryId(),
                po.getBrand(),
                po.getCoverImageUrl(),
                po.getStockTotal(),
                po.getSaleCount(),
                SkuType.from(po.getSkuType()),
                ProductStatus.from(po.getStatus()),
                po.getDefaultSkuId(),
                parseStringList(po.getTags()),
                po.getUpdatedAt()
        );
    }

    /**
     * 将领域实体转换为持久化对象
     *
     * @param entity 商品实体
     * @return 持久化对象
     */
    private ProductPO toPo(Product entity) {
        return ProductPO.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .title(entity.getTitle())
                .subtitle(entity.getSubtitle())
                .description(entity.getDescription())
                .categoryId(entity.getCategoryId())
                .brand(entity.getBrand())
                .coverImageUrl(entity.getCoverImageUrl())
                .stockTotal(entity.getStockTotal())
                .saleCount(entity.getSaleCount())
                .skuType(entity.getSkuType() == null ? null : entity.getSkuType().name())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .defaultSkuId(entity.getDefaultSkuId())
                .tags(toJson(entity.getTags()))
                .build();
    }

    /**
     * 将规格 PO 转换为领域对象
     *
     * @param po 规格 PO
     * @return 规格
     */
    private ProductSpec toProductSpec(ProductSpecPO po) {
        return ProductSpec.reconstitute(po.getId(), po.getProductId(), po.getSpecCode(), po.getSpecName(),
                SpecType.from(po.getSpecType()), po.getIsRequired() != null && po.getIsRequired() == 1,
                po.getSortOrder(), !"DISABLED".equalsIgnoreCase(po.getStatus()));
    }

    /**
     * 将规格值 PO 转换为领域对象
     *
     * @param po 规格值 PO
     * @return 规格值
     */
    private ProductSpecValue toProductSpecValue(ProductSpecValuePO po) {
        return ProductSpecValue.reconstitute(po.getId(), po.getProductId(), po.getSpecId(), po.getValueCode(),
                po.getValueName(), parseAttributes(po.getAttributes()), po.getSortOrder(),
                !"DISABLED".equalsIgnoreCase(po.getStatus()));
    }

    /**
     * 将 SKU PO 转换为领域对象
     *
     * @param po SKU PO
     * @return SKU
     */
    private ProductSku toProductSku(ProductSkuPO po) {
        return ProductSku.reconstitute(
                po.getId(),
                po.getProductId(),
                po.getSkuCode(),
                po.getStock(),
                po.getWeight(),
                SkuStatus.from(po.getStatus()),
                po.getIsDefault() != null && po.getIsDefault() == 1,
                po.getBarcode()
        );
    }

    /**
     * 将 ProductImagePO 转换为图片 VO
     *
     * @param po 持久化对象
     * @return 图片 VO
     */
    private ProductImage toProductImage(ProductImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    /**
     * 将 SKU Image PO 转换为图片 VO
     *
     * @param po 持久化对象
     * @return 图片 VO
     */
    private ProductImage toProductImage(ProductSkuImagePO po) {
        return ProductImage.of(po.getUrl(), po.getIsMain() != null && po.getIsMain() == 1, po.getSortOrder());
    }

    /**
     * 将 ProductI18nPO 转换为 i18n VO
     *
     * @param po 持久化对象
     * @return i18n VO
     */
    private ProductI18n toProductI18n(ProductI18nPO po) {
        return ProductI18n.of(po.getLocale(), po.getTitle(), po.getSubtitle(), po.getDescription(),
                po.getSlug(), parseStringList(po.getTags()));
    }

    /**
     * 将价格 PO 转换为 VO
     *
     * @param po 价格 PO
     * @return 价格 VO
     */
    private ProductPrice toProductPrice(ProductPricePO po) {
        if (po.getCurrency() == null || po.getListPrice() == null) {
            log.warn("价格记录缺少必填字段, skuId={}", po.getSkuId());
            return null;
        }
        return ProductPrice.of(po.getCurrency(), po.getListPrice(), po.getSalePrice(), po.getIsActive() != null && po.getIsActive() == 1);
    }

    /**
     * 批量 upsert 规格 i18n
     *
     * @param specId   规格 ID
     * @param i18nList i18n 列表
     */
    private void upsertSpecI18n(Long specId, List<ProductSpecI18n> i18nList) {
        if (specId == null || i18nList == null || i18nList.isEmpty())
            return;
        // 根据 Spec ID 获取 locale -> Spec I18N 映射
        Map<String, ProductSpecI18nPO> existing = productSpecI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecI18nPO>()
                        .eq(ProductSpecI18nPO::getSpecId, specId))
                .stream()
                .collect(Collectors.toMap(ProductSpecI18nPO::getLocale, Function.identity(),
                        (current, ignore) -> current, LinkedHashMap::new));
        for (ProductSpecI18n i18n : i18nList) {
            ProductSpecI18nPO po = existing.get(i18n.getLocale());
            if (po == null) {
                productSpecI18nMapper.insert(ProductSpecI18nPO.builder()
                        .specId(specId)
                        .locale(i18n.getLocale())
                        .specName(i18n.getSpecName())
                        .build());
            } else {
                po.setSpecName(i18n.getSpecName());
                productSpecI18nMapper.updateById(po);
            }
        }
    }

    /**
     * 批量 upsert 规格值 i18n
     *
     * @param valueId  规格值 ID
     * @param i18nList i18n 列表
     */
    private void upsertSpecValueI18n(Long valueId, List<ProductSpecValueI18n> i18nList) {
        if (valueId == null || i18nList == null || i18nList.isEmpty())
            return;
        Map<String, ProductSpecValueI18nPO> existing = productSpecValueI18nMapper.selectList(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                        .eq(ProductSpecValueI18nPO::getValueId, valueId))
                .stream()
                .collect(Collectors.toMap(ProductSpecValueI18nPO::getLocale, Function.identity(),
                        (current, ignore) -> current, LinkedHashMap::new));
        for (ProductSpecValueI18n i18n : i18nList) {
            ProductSpecValueI18nPO po = existing.get(i18n.getLocale());
            if (po == null) {
                productSpecValueI18nMapper.insert(ProductSpecValueI18nPO.builder()
                        .valueId(valueId)
                        .locale(i18n.getLocale())
                        .valueName(i18n.getValueName())
                        .build());
            } else {
                po.setValueName(i18n.getValueName());
                productSpecValueI18nMapper.updateById(po);
            }
        }
    }

    /**
     * upsert 价格
     *
     * @param skuId   SKU ID
     * @param command 价格命令
     */
    private void upsertPriceInternal(Long skuId, ProductPriceUpsertCommand command) {
        ProductPricePO existing = productPriceMapper.selectOne(new LambdaQueryWrapper<ProductPricePO>()
                .eq(ProductPricePO::getSkuId, skuId)
                .eq(ProductPricePO::getCurrency, command.getCurrency()));
        if (existing == null) {
            productPriceMapper.insert(ProductPricePO.builder()
                    .skuId(skuId)
                    .currency(command.getCurrency())
                    .listPrice(command.getListPrice())
                    .salePrice(command.getSalePrice())
                    .isActive(command.isActive() ? 1 : 0)
                    .build());
            return;
        }
        existing.setListPrice(command.getListPrice());
        existing.setSalePrice(command.getSalePrice());
        existing.setIsActive(command.isActive() ? 1 : 0);
        productPriceMapper.updateById(existing);
    }

    /**
     * 批量 upsert 价格
     *
     * @param skuId    SKU ID
     * @param commands 价格命令列表
     */
    private void upsertPriceListInternal(Long skuId, List<ProductPriceUpsertCommand> commands) {
        if (commands == null || commands.isEmpty())
            return;
        for (ProductPriceUpsertCommand command : commands) {
            if (command == null)
                continue;
            upsertPriceInternal(skuId, command);
        }
    }

    /**
     * 替换 SKU 规格绑定
     *
     * @param skuId SKU ID
     * @param specs 规格列表
     */
    private void replaceSkuSpecs(Long skuId, List<ProductSkuSpecUpsertCommand> specs) {
        productSkuSpecMapper.delete(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .eq(ProductSkuSpecPO::getSkuId, skuId));
        if (specs == null || specs.isEmpty())
            return;
        for (ProductSkuSpecUpsertCommand spec : specs) {
            productSkuSpecMapper.insert(ProductSkuSpecPO.builder()
                    .skuId(skuId)
                    .specId(spec.getSpecId())
                    .valueId(spec.getValueId())
                    .build());
        }
    }

    /**
     * 替换 SKU 图片
     *
     * @param skuId  SKU ID
     * @param images 图片列表
     */
    private void replaceSkuImages(Long skuId, List<ProductImage> images) {
        productSkuImageMapper.delete(new LambdaQueryWrapper<ProductSkuImagePO>()
                .eq(ProductSkuImagePO::getSkuId, skuId));
        if (images == null || images.isEmpty())
            return;
        for (ProductImage image : images) {
            productSkuImageMapper.insert(ProductSkuImagePO.builder()
                    .skuId(skuId)
                    .url(image.getUrl())
                    .isMain(image.isMain() ? 1 : 0)
                    .sortOrder(image.getSortOrder())
                    .build());
        }
    }

    /**
     * 将标签列表转换为 JSON
     *
     * @param tags 标签列表
     * @return JSON 字符串或 null
     */
    private String toJson(@Nullable List<String> tags) {
        if (tags == null || tags.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception ex) {
            log.warn("标签序列化失败, 使用空列表代替", ex);
            return "[]";
        }
    }

    /**
     * 将 Map 转换为 JSON
     *
     * @param attributes 属性
     * @return JSON 字符串
     */
    private String toJson(@Nullable Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (Exception ex) {
            log.warn("规格属性序列化失败, 使用空对象代替", ex);
            return "{}";
        }
    }

    /**
     * 解析 JSON 为字符串列表
     *
     * @param json JSON 字符串
     * @return 字符串列表
     */
    private List<String> parseStringList(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("标签解析失败, 返回空列表, 原始数据: {}", json, ex);
            return Collections.emptyList();
        }
    }

    /**
     * 解析 JSON 为属性 Map
     *
     * @param json JSON 字符串
     * @return 属性 Map
     */
    private Map<String, Object> parseAttributes(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("规格属性解析失败, 返回空映射, 原始数据: {}", json, ex);
            return Collections.emptyMap();
        }
    }
}
