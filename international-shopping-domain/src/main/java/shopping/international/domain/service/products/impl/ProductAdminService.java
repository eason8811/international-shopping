package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductAdminRepository;
import shopping.international.domain.adapter.repository.products.IProductCategoryRepository;
import shopping.international.domain.model.entity.products.Category;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.entity.products.ProductSku;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.enums.products.CategoryStatus;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.*;
import shopping.international.domain.service.products.IProductAdminService;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
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
     * 规格/编码最大长度
     */
    private static final int SPEC_MAX = 64;
    /**
     * 品牌最大长度
     */
    private static final int BRAND_MAX = 120;
    /**
     * 图片 URL 最大长度
     */
    private static final int IMAGE_MAX = 500;
    /**
     * SKU 编码最大长度
     */
    private static final int SKU_CODE_MAX = 64;
    /**
     * 条码最大长度
     */
    private static final int BARCODE_MAX = 64;
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
     * 增量维护商品规格
     *
     * @param productId       商品 ID
     * @param specCommandList 规格命令列表
     * @return 更新后的商品详情
     */
    @Override
    public @NotNull ProductDetail upsertSpecs(@NotNull Long productId, @NotNull List<ProductSpecUpsertCommand> specCommandList) {
        ensureProduct(productId);
        if (specCommandList.isEmpty())
            return loadDetail(productId);

        // 规范化 Spec Command 列表
        SpecContext context = buildSpecContext(productId);
        Set<String> payloadSpecCodes = new LinkedHashSet<>();
        Set<String> payloadSpecNames = new LinkedHashSet<>();
        List<ProductSpecUpsertCommand> normalizedSpecList = new ArrayList<>();
        int specSort = 0;
        // 遍历传入的 更新插入命令
        for (ProductSpecUpsertCommand specCommand : specCommandList) {
            Long specId = specCommand.getSpecId();
            String specCode = normalizeSpecCode(specCommand.getSpecCode());
            String specName = normalizeSpecName(specCommand.getSpecName());
            // 如果传入的 Spec ID 不为空, 则检查在现存的规格中查找有没有相同 ID 的 Spec
            if (specId != null && !context.specById().containsKey(specId))
                throw new IllegalParamException("规格不存在");
            // 检查传入的 Spec Code 是否重复, 如果存在重复, 则检查这个重复的 Spec ID 是否和传入的一致, 不一致则不允许重复, 一致则视为更新, 允许重复
            ProductSpec conflictedCode = context.specByCode().get(specCode);
            if (conflictedCode != null && !conflictedCode.getId().equals(specId))
                throw new IllegalParamException("规格编码已存在");
            // 检查是否有名称重复但是 Spec ID 不相同的情况
            boolean nameConflict = context.specById().values().stream()
                    .anyMatch(item -> item.getSpecName().equals(specName) && !item.getId().equals(specId));
            if (nameConflict)
                throw new IllegalParamException("规格名称已存在");
            if (!payloadSpecCodes.add(specCode))
                throw new IllegalParamException("规格编码重复");
            if (!payloadSpecNames.add(specName))
                throw new IllegalParamException("规格名称重复");

            // 在 context 中根据 Spec ID 获取 Spec Value 列表, 并映射为 Code 集合和 Name 集合
            Set<String> valueCodes = new LinkedHashSet<>();
            Set<String> valueNames = new LinkedHashSet<>();
            if (specId != null) {
                List<ProductSpecValue> existingValues = context.valuesBySpecId().getOrDefault(specId, List.of());
                existingValues.forEach(val -> {
                    valueCodes.add(val.getValueCode());
                    valueNames.add(val.getValueName());
                });
            }

            // 规范化 Spec Value Command 列表
            List<ProductSpecValueUpsertCommand> valueCommandList = specCommand.getValues() == null
                    ? List.of()
                    : specCommand.getValues();
            List<ProductSpecValueUpsertCommand> normalizedValueList = new ArrayList<>();
            int valueSort = 0;
            for (ProductSpecValueUpsertCommand valueCommand : valueCommandList) {
                Long valueId = valueCommand.getValueId();
                if (specId == null && valueId != null)
                    throw new IllegalParamException("新规格的规格值不允许携带 ID");
                if (valueId != null) {
                    // 在 context 中根据 Spec Value ID 获取 Value, 如果传入的 Value ID 不为空, 则根据此 ID 从 context 中查找 Spec Value, 如果不存在或不属于当前 Spec, 则抛出异常
                    ProductSpecValue existingValue = context.valueById().get(valueId);
                    if (existingValue == null || !Objects.equals(existingValue.getSpecId(), specId))
                        throw new IllegalParamException("规格值不存在");
                    // 没有异常则从 Spec Value 的 Code 的集合和 Name 集合中移除已存在的 Value
                    valueCodes.remove(existingValue.getValueCode());
                    valueNames.remove(existingValue.getValueName());
                }
                // 然后将规范化后的 Spec Value 的 Code 和 Name 添加到集合中
                String valueCode = normalizeValueCode(valueCommand.getValueCode());
                String valueName = normalizeValueName(valueCommand.getValueName());
                if (!valueCodes.add(valueCode))
                    throw new IllegalParamException("规格值编码重复");
                if (!valueNames.add(valueName))
                    throw new IllegalParamException("规格值名称重复");
                normalizedValueList.add(
                        ProductSpecValueUpsertCommand.of(
                                valueId,
                                specId,
                                valueCode,
                                valueName,
                                valueCommand.getAttributes(),
                                valueCommand.isEnabled(),
                                valueSort++,
                                valueCommand.getI18nList()
                        )
                );
            }
            normalizedSpecList.add(
                    ProductSpecUpsertCommand.of(
                            specId,
                            specCode,
                            specName,
                            specCommand.getSpecType(),
                            specCommand.isRequired(),
                            specCommand.isEnabled(),
                            specSort++,
                            specCommand.getI18nList(),
                            normalizedValueList
                    )
            );
        }
        productAdminRepository.upsertSpecs(productId, normalizedSpecList);
        return loadDetail(productId);
    }

    /**
     * 批量创建 SKU
     *
     * @param productId 商品 ID
     * @param command   SKU 创建命令
     * @return 商品详情
     */
    @Override
    public @NotNull ProductDetail createSkus(@NotNull Long productId, @NotNull ProductSkuUpsertCommand command) {
        Product product = ensureProduct(productId);
        List<ProductSkuUpsertItemCommand> itemList = command.getItems();
        if (itemList == null || itemList.isEmpty())
            throw new IllegalParamException("SKU 列表不能为空");
        List<ProductSku> existingSkuList = productAdminRepository.listSkus(productId);
        if (product.getSkuType() == SkuType.SINGLE && (!existingSkuList.isEmpty() || itemList.size() > 1))
            throw new IllegalParamException("单规格商品仅允许一个 SKU");

        SpecContext context = buildSpecContext(productId);
        Set<String> usedCodes = existingSkuList.stream()
                .map(ProductSku::getSkuCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ProductSkuUpsertItemCommand> normalized = new ArrayList<>();
        String defaultCode = null;
        for (ProductSkuUpsertItemCommand item : itemList) {
            if (item.getId() != null)
                throw new IllegalParamException("创建 SKU 时不应携带 ID");
            String skuCode = normalizeSkuCode(item.getSkuCode());
            if (!usedCodes.add(skuCode) || productAdminRepository.existsSkuCode(skuCode, null))
                throw new IllegalParamException("SKU 编码已存在");
            if (item.getStatus() == null)
                throw new IllegalParamException("SKU 状态不能为空");
            if (item.getWeight() != null && item.getWeight().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalParamException("SKU 重量不能为负数");
            if (item.getStock() < 0)
                throw new IllegalParamException("SKU 库存不能为负数");
            boolean isDefault = item.isDefault();
            if (isDefault) {
                if (defaultCode != null)
                    throw new IllegalParamException("默认 SKU 只能有一个");
                defaultCode = skuCode;
            }
            String barcode = normalizeBarcode(item.getBarcode());
            List<ProductSkuSpecUpsertCommand> resolvedSpecList = resolveSkuSpecs(item.getSpecs(), context, product.getSkuType());
            List<ProductImage> imageList = normalizeSkuImages(item.getImages());
            normalized.add(ProductSkuUpsertItemCommand.of(null, skuCode, item.getStock(), item.getWeight(),
                    item.getStatus(), isDefault, barcode, item.getPrice(), resolvedSpecList, imageList));
        }
        List<ProductSku> created = productAdminRepository.createSkus(productId, normalized);
        Long targetDefault = product.getDefaultSkuId();
        if (defaultCode != null) {
            String finalDefaultCode = defaultCode;
            targetDefault = created.stream()
                    .filter(sku -> finalDefaultCode.equals(sku.getSkuCode()))
                    .map(ProductSku::getId)
                    .findFirst()
                    .orElse(targetDefault);
        }
        if (targetDefault == null) {
            targetDefault = existingSkuList.stream()
                    .filter(ProductSku::isDefault)
                    .map(ProductSku::getId)
                    .findFirst()
                    .orElseGet(() -> created.isEmpty() ? null : created.get(0).getId());
        }
        int stockTotal = productAdminRepository.sumStock(productId);
        productAdminRepository.updateProductStockAndDefault(productId, stockTotal, targetDefault);
        return loadDetail(productId);
    }

    /**
     * 批量更新 SKU
     *
     * @param productId 商品 ID
     * @param command   SKU 更新命令
     * @return 商品详情
     */
    @Override
    public @NotNull ProductDetail updateSkus(@NotNull Long productId, @NotNull ProductSkuUpsertCommand command) {
        Product product = ensureProduct(productId);
        List<ProductSkuUpsertItemCommand> items = command.getItems();
        if (items == null || items.isEmpty())
            throw new IllegalParamException("SKU 列表不能为空");
        List<ProductSku> existingSkus = productAdminRepository.listSkus(productId);
        if (product.getSkuType() == SkuType.SINGLE && items.size() > 1)
            throw new IllegalParamException("单规格商品仅允许一个 SKU");
        Map<Long, ProductSku> skuById = existingSkus.stream()
                .collect(Collectors.toMap(ProductSku::getId, Function.identity(), (current, ignore) -> current, LinkedHashMap::new));
        SpecContext context = buildSpecContext(productId);
        Set<String> payloadCodes = new LinkedHashSet<>();
        List<ProductSkuUpsertItemCommand> normalized = new ArrayList<>();
        Long defaultCandidate = null;
        boolean defaultSpecified = false;
        for (ProductSkuUpsertItemCommand item : items) {
            if (item.getId() == null || !skuById.containsKey(item.getId()))
                throw new IllegalParamException("SKU 不存在");
            String skuCode = normalizeSkuCode(item.getSkuCode());
            if (!payloadCodes.add(skuCode))
                throw new IllegalParamException("SKU 编码重复");
            if (productAdminRepository.existsSkuCode(skuCode, item.getId()))
                throw new IllegalParamException("SKU 编码已存在");
            if (item.getStatus() == null)
                throw new IllegalParamException("SKU 状态不能为空");
            if (item.getWeight() != null && item.getWeight().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalParamException("SKU 重量不能为负数");
            if (item.getStock() < 0)
                throw new IllegalParamException("SKU 库存不能为负数");
            boolean isDefault = item.isDefault();
            if (isDefault) {
                if (defaultCandidate != null)
                    throw new IllegalParamException("默认 SKU 只能有一个");
                defaultCandidate = item.getId();
                defaultSpecified = true;
            } else if (product.getDefaultSkuId() != null && product.getDefaultSkuId().equals(item.getId())) {
                defaultCandidate = null;
                defaultSpecified = true;
            }
            String barcode = normalizeBarcode(item.getBarcode());
            List<ProductSkuSpecUpsertCommand> resolvedSpecLis = resolveSkuSpecs(item.getSpecs(), context, product.getSkuType());
            List<ProductImage> imageList = normalizeSkuImages(item.getImages());
            normalized.add(ProductSkuUpsertItemCommand.of(item.getId(), skuCode, item.getStock(), item.getWeight(),
                    item.getStatus(), isDefault, barcode, item.getPrice(), resolvedSpecLis, imageList));
        }
        List<ProductSku> updated = productAdminRepository.updateSkus(productId, normalized);
        Long targetDefault = defaultSpecified ? defaultCandidate : product.getDefaultSkuId();
        if (targetDefault == null)
            targetDefault = updated.stream().findFirst()
                    .map(ProductSku::getId)
                    .orElseGet(() -> existingSkus.stream().findFirst().map(ProductSku::getId).orElse(null));
        int stockTotal = productAdminRepository.sumStock(productId);
        productAdminRepository.updateProductStockAndDefault(productId, stockTotal, targetDefault);
        return loadDetail(productId);
    }

    /**
     * 更新 SKU 价格
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   价格命令
     * @return 更新后的 SKU
     */
    @Override
    public @NotNull ProductSku updateSkuPrice(@NotNull Long productId, @NotNull Long skuId, @NotNull ProductPriceUpsertCommand command) {
        ensureProduct(productId);
        ProductSku sku = productAdminRepository.findSkuById(skuId)
                .orElseThrow(() -> IllegalParamException.of("SKU 不存在"));
        if (!Objects.equals(productId, sku.getProductId()))
            throw new IllegalParamException("SKU 不属于当前商品");
        ProductSku updated = productAdminRepository.upsertPrice(productId, skuId, command);
        updated.attachPrice(ProductPrice.of(command.getCurrency(), command.getListPrice(), command.getSalePrice(), command.isActive()));
        List<ProductSku> attached = attachSkuDetails(productId, List.of(updated));
        return attached.isEmpty() ? updated : attached.get(0);
    }

    /**
     * 调整 SKU 库存
     *
     * @param productId 商品 ID
     * @param skuId     SKU ID
     * @param command   库存调整命令
     * @return 更新后的 SKU
     */
    @Override
    public @NotNull ProductSku adjustSkuStock(@NotNull Long productId, @NotNull Long skuId, @NotNull StockAdjustCommand command) {
        Product product = ensureProduct(productId);
        ProductSku sku = productAdminRepository.findSkuById(skuId)
                .orElseThrow(() -> IllegalParamException.of("SKU 不存在"));
        if (!Objects.equals(productId, sku.getProductId()))
            throw new IllegalParamException("SKU 不属于当前商品");
        ProductSku updated = productAdminRepository.adjustStock(productId, skuId, command);
        int stockTotal = productAdminRepository.sumStock(productId);
        productAdminRepository.updateProductStockAndDefault(productId, stockTotal, product.getDefaultSkuId());
        List<ProductSku> attached = attachSkuDetails(productId, List.of(updated));
        return attached.isEmpty() ? updated : attached.get(0);
    }

    /**
     * 组装列表使用的概要视图
     *
     * @param product     商品实体
     * @param categoryMap 分类映射
     * @param galleryMap  图库映射
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
     * @param command  包含了从客户端接收的商品信息的命令对象
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
     * 根据商品 ID 获取商品实体
     *
     * @param productId 商品 ID
     * @return 商品实体
     */
    private Product ensureProduct(@NotNull Long productId) {
        return productAdminRepository.findById(productId)
                .orElseThrow(() -> IllegalParamException.of("商品不存在"));
    }

    /**
     * 构建规格上下文索引
     *
     * @param productId 商品 ID
     * @return 规格上下文
     */
    private SpecContext buildSpecContext(Long productId) {
        List<ProductSpec> specs = productAdminRepository.listSpecs(productId, true);
        Map<Long, ProductSpec> specById = specs.stream()
                .collect(Collectors.toMap(ProductSpec::getId, Function.identity(), (current, ignore) -> current, LinkedHashMap::new));
        Map<String, ProductSpec> specByCode = specs.stream()
                .collect(Collectors.toMap(ProductSpec::getSpecCode, Function.identity(), (current, ignore) -> current, LinkedHashMap::new));
        List<ProductSpecValue> values = productAdminRepository.listSpecValues(productId, true);
        Map<Long, ProductSpecValue> valueById = values.stream()
                .collect(Collectors.toMap(ProductSpecValue::getId, Function.identity(), (current, ignore) -> current, LinkedHashMap::new));
        Map<Long, List<ProductSpecValue>> valuesBySpecId = values.stream()
                .collect(Collectors.groupingBy(ProductSpecValue::getSpecId, LinkedHashMap::new, Collectors.toList()));
        return new SpecContext(specById, specByCode, valueById, valuesBySpecId);
    }

    /**
     * 标准化 SKU 图片列表
     *
     * @param images 图片列表
     * @return 处理后的图片列表
     */
    private List<ProductImage> normalizeSkuImages(List<ProductImage> images) {
        return normalizeGallery(images);
    }

    /**
     * 组装 SKU 细节
     *
     * @param productId 商品 ID
     * @param skus      SKU 列表
     * @return 组装后的 SKU 列表
     */
    private List<ProductSku> attachSkuDetails(Long productId, List<ProductSku> skus) {
        if (skus == null || skus.isEmpty())
            return List.of();
        Set<Long> skuIds = skus.stream().map(ProductSku::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ProductPrice> priceMap = productAdminRepository.mapLatestPrices(skuIds);
        Map<Long, List<ProductImage>> imageMap = productAdminRepository.mapSkuImages(skuIds);
        Map<Long, List<ProductSkuSpec>> skuSpecMap = productAdminRepository.mapSkuSpecs(skuIds);
        return skus.stream()
                .peek(sku -> {
                    ProductPrice price = priceMap.get(sku.getId());
                    if (price != null)
                        sku.attachPrice(price);
                    sku.attachImages(imageMap.getOrDefault(sku.getId(), List.of()));
                    sku.attachSpecs(skuSpecMap.getOrDefault(sku.getId(), List.of()));
                })
                .sorted(Comparator.comparing(ProductSku::isDefault).reversed().thenComparing(ProductSku::getId))
                .toList();
    }

    /**
     * 构建规格的唯一键
     *
     * @param specId   规格 ID
     * @param specCode 规格编码
     * @return 唯一键
     */
    private String buildSpecKey(Long specId, String specCode) {
        return specId != null ? "ID:" + specId : "CODE:" + specCode;
    }

    /**
     * 标准化规格编码
     *
     * @param code 规格编码
     * @return 规范化规格编码
     */
    private String normalizeSpecCode(String code) {
        requireNotBlank(code, "规格编码不能为空");
        String normalized = code.strip();
        if (normalized.length() > SPEC_MAX)
            throw new IllegalParamException("规格编码长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 标准化规格名称
     *
     * @param name 规格名称
     * @return 规范化规格名称
     */
    private String normalizeSpecName(String name) {
        requireNotBlank(name, "规格名称不能为空");
        String normalized = name.strip();
        if (normalized.length() > SPEC_MAX)
            throw new IllegalParamException("规格名称长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 标准化规格值编码
     *
     * @param code 规格值编码
     * @return 规范化规格值编码
     */
    private String normalizeValueCode(String code) {
        requireNotBlank(code, "规格值编码不能为空");
        String normalized = code.strip();
        if (normalized.length() > SPEC_MAX)
            throw new IllegalParamException("规格值编码长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 标准化规格值名称
     *
     * @param name 规格值名称
     * @return 规范化规格值名称
     */
    private String normalizeValueName(String name) {
        requireNotBlank(name, "规格值名称不能为空");
        String normalized = name.strip();
        if (normalized.length() > SPEC_MAX)
            throw new IllegalParamException("规格值名称长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 标准化 SKU 编码
     *
     * @param skuCode SKU 编码
     * @return 规范化 SKU 编码
     */
    private String normalizeSkuCode(String skuCode) {
        requireNotBlank(skuCode, "SKU 编码不能为空");
        String normalized = skuCode.strip();
        if (normalized.length() > SKU_CODE_MAX)
            throw new IllegalParamException("SKU 编码长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 标准化条码
     *
     * @param barcode 条码
     * @return 规范化条码
     */
    private String normalizeBarcode(String barcode) {
        if (barcode == null)
            return null;
        String normalized = barcode.strip();
        if (normalized.isEmpty())
            return null;
        if (normalized.length() > BARCODE_MAX)
            throw new IllegalParamException("条码长度不能超过 64 个字符");
        return normalized;
    }

    /**
     * 解析 SKU 的规格绑定
     *
     * @param specList 规格绑定列表
     * @param context  规格上下文
     * @param skuType  SKU 类型
     * @return 规格绑定命令列表
     */
    private List<ProductSkuSpecUpsertCommand> resolveSkuSpecs(List<ProductSkuSpecUpsertCommand> specList,
                                                              SpecContext context,
                                                              SkuType skuType) {
        if (specList == null || specList.isEmpty())
            return List.of();
        if (skuType == SkuType.SINGLE)
            throw new IllegalParamException("单规格商品不需要关联规格");
        List<ProductSkuSpecUpsertCommand> normalized = new ArrayList<>();
        Set<Long> specIds = new LinkedHashSet<>();
        // 解析 SKU 和规格的绑定关系 Command
        for (ProductSkuSpecUpsertCommand spec : specList) {
            String specCode = normalizeSpecCode(spec.getSpecCode());
            ProductSpec specEntity = spec.getSpecId() != null
                    ? context.specById().get(spec.getSpecId())
                    : context.specByCode().get(specCode);
            if (specEntity == null)
                throw new IllegalParamException("规格不存在");
            Long resolvedSpecId = specEntity.getId();
            if (!specIds.add(resolvedSpecId))
                throw new IllegalParamException("SKU 规格不可重复");
            ProductSpecValue valueEntity;
            if (spec.getValueId() != null) {
                valueEntity = context.valueById().get(spec.getValueId());
                if (valueEntity == null || !Objects.equals(valueEntity.getSpecId(), resolvedSpecId))
                    throw new IllegalParamException("规格值不存在");
            } else {
                List<ProductSpecValue> candidates = context.valuesBySpecId().getOrDefault(resolvedSpecId, List.of());
                valueEntity = candidates.stream()
                        .filter(val -> normalizeValueCode(spec.getValueCode()).equals(val.getValueCode()))
                        .findFirst()
                        .orElseThrow(() -> IllegalParamException.of("规格值不存在"));
            }
            normalized.add(ProductSkuSpecUpsertCommand.of(resolvedSpecId, specCode, valueEntity.getId(), valueEntity.getValueCode()));
        }
        return normalized;
    }

    /**
     * 规格上下文
     *
     * @param specById       规格 ID 索引 (spec ID -> 规格)
     * @param specByCode     规格编码索引 (spec CODE -> 规格)
     * @param valueById      规格值 ID 索引 (spec value ID -> 规格值)
     * @param valuesBySpecId 规格值分组 (spec ID -> 规格值列表)
     */
    private record SpecContext(Map<Long, ProductSpec> specById,
                               Map<String, ProductSpec> specByCode,
                               Map<Long, ProductSpecValue> valueById,
                               Map<Long, List<ProductSpecValue>> valuesBySpecId) {
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
        List<ProductSpec> specs = productAdminRepository.listSpecs(productId, true);
        List<ProductSpecValue> specValues = productAdminRepository.listSpecValues(productId, true);
        Map<Long, List<ProductSpecValue>> specValueMap = specValues.stream()
                .collect(Collectors.groupingBy(ProductSpecValue::getSpecId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> specIds = specs.stream().map(ProductSpec::getId).collect(Collectors.toSet());
        Set<Long> valueIds = specValues.stream().map(ProductSpecValue::getId).collect(Collectors.toSet());
        Map<Long, List<ProductSpecI18n>> specI18nMap = specIds.isEmpty() ? Map.of() : productAdminRepository.mapSpecI18n(specIds);
        Map<Long, List<ProductSpecValueI18n>> specValueI18nMap = valueIds.isEmpty() ? Map.of() : productAdminRepository.mapSpecValueI18n(valueIds);
        specs.forEach(spec -> {
            spec.attachValues(specValueMap.getOrDefault(spec.getId(), List.of()));
            spec.attachI18nList(specI18nMap.getOrDefault(spec.getId(), List.of()));
        });
        specValues.forEach(value -> value.attachI18nList(specValueI18nMap.getOrDefault(value.getId(), List.of())));
        List<ProductSpec> orderedSpecs = specs.stream()
                .sorted(Comparator.comparingInt(ProductSpec::getSortOrder).thenComparing(ProductSpec::getId))
                .toList();
        List<ProductSku> skus = attachSkuDetails(productId, productAdminRepository.listSkus(productId));
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
                orderedSpecs,
                skus,
                primaryI18n,
                i18nList
        );
    }
}
