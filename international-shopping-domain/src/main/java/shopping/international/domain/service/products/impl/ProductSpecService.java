package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.adapter.repository.products.IProductSpecRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.domain.service.products.IProductSpecService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 商品规格领域服务实现
 *
 * <p>处理规格与规格值的增删改查编排, 并在删除前校验 SKU 绑定。</p>
 */
@Service
@RequiredArgsConstructor
public class ProductSpecService implements IProductSpecService {

    /**
     * 商品聚合仓储
     */
    private final IProductRepository productRepository;
    /**
     * 规格仓储
     */
    private final IProductSpecRepository productSpecRepository;

    /**
     * 列出商品下全部规格
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    @Override
    public @NotNull List<ProductSpec> list(@NotNull Long productId) {
        ensureProduct(productId);
        return productSpecRepository.listByProductId(productId);
    }

    /**
     * 创建规格
     *
     * @param productId 商品 ID
     * @param spec      规格实体
     * @return 新增的规格 ID
     */
    @Override
    public @NotNull Long create(@NotNull Long productId, @NotNull ProductSpec spec) {
        Product product = ensureProduct(productId);
        product.addSpec(spec);
        ProductSpec saved = productSpecRepository.save(spec);
        return saved.getId();
    }

    /**
     * 更新规格
     *
     * @param productId   商品 ID
     * @param specId      规格 ID
     * @param specName    新规格名, 可空
     * @param specType    新规格类型, 可空
     * @param required    是否必选, 可空
     * @param sortOrder   排序, 可空
     * @param enabled     是否启用, 可空
     * @param i18nList    多语言列表, 可空
     * @param patchI18n 是否覆盖 i18n
     * @return 规格 ID
     */
    @Override
    public @NotNull Long update(@NotNull Long productId, @NotNull Long specId, @Nullable String specName,
                                @Nullable SpecType specType, @Nullable Boolean required, @Nullable Integer sortOrder,
                                @Nullable Boolean enabled, @Nullable List<ProductSpecI18n> i18nList, boolean patchI18n) {
        Product product = ensureProduct(productId);
        ProductSpec spec = ensureSpec(productId, specId);
        product.updateSpec(specId, specName, specType, required, sortOrder, enabled);
        spec.update(specName, specType, required, sortOrder, enabled);
        if (patchI18n && i18nList != null)
            spec.updateI18nBatch(i18nList);
        ProductSpec updated = productSpecRepository.update(spec, patchI18n);
        return updated.getId();
    }

    /**
     * 删除规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    @Override
    public boolean delete(@NotNull Long productId, @NotNull Long specId) {
        ensureSpec(productId, specId);
        if (productSpecRepository.hasSkuBindingsForSpec(specId))
            throw new ConflictException("存在 SKU 绑定, 无法删除规格");
        return productSpecRepository.deleteSpec(productId, specId);
    }

    /**
     * 列出规格取值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格值列表
     */
    @Override
    public @NotNull List<ProductSpecValue> listValues(@NotNull Long productId, @NotNull Long specId) {
        ensureSpec(productId, specId);
        return productSpecRepository.listValues(productId, specId);
    }

    /**
     * 创建规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param value     规格值
     * @return 新增的规格值 ID
     */
    @Override
    public @NotNull Long createValue(@NotNull Long productId, @NotNull Long specId, @NotNull ProductSpecValue value) {
        ProductSpec spec = ensureSpec(productId, specId);
        spec.addValue(value);
        ProductSpecValue saved = productSpecRepository.saveValue(value);
        return saved.getId();
    }

    /**
     * 更新规格值
     *
     * @param productId   商品 ID
     * @param specId      规格 ID
     * @param valueId     规格值 ID
     * @param valueCode   新编码, 可空
     * @param valueName   新名称, 可空
     * @param attributes  新属性, 可空
     * @param sortOrder   新排序, 可空
     * @param enabled     是否启用, 可空
     * @param i18nList    多语言列表, 可空
     * @param patchI18n 是否覆盖 i18n
     * @return 规格值 ID
     */
    @Override
    public @NotNull Long updateValue(@NotNull Long productId, @NotNull Long specId, @NotNull Long valueId,
                                     @Nullable String valueCode, @Nullable String valueName, @Nullable Map<String, Object> attributes,
                                     @Nullable Integer sortOrder, @Nullable Boolean enabled,
                                     @Nullable List<ProductSpecValueI18n> i18nList, boolean patchI18n) {
        ProductSpec spec = ensureSpec(productId, specId);
        ProductSpecValue specValue = spec.getValues().stream()
                .filter(item -> Objects.equals(item.getId(), valueId))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("规格值不存在"));
        String targetCode = valueCode == null ? specValue.getValueCode() : valueCode;
        spec.updateValue(targetCode, valueName, attributes, sortOrder, enabled);
        if (patchI18n && i18nList != null)
            specValue.updateI18nBatch(i18nList);
        ProductSpecValue updated = productSpecRepository.updateValue(specValue, patchI18n);
        return updated.getId();
    }

    /**
     * 删除规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @param valueId   规格值 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteValue(@NotNull Long productId, @NotNull Long specId, @NotNull Long valueId) {
        ensureSpec(productId, specId);
        if (productSpecRepository.hasSkuBindingsForValue(valueId))
            throw new ConflictException("存在 SKU 绑定, 无法删除规格值");
        return productSpecRepository.deleteValue(specId, valueId);
    }

    /**
     * 校验商品存在且未被删除
     *
     * @param productId 商品 ID
     * @return 商品聚合
     */
    private @NotNull Product ensureProduct(@NotNull Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalParamException("商品不存在"));
        if (product.getStatus() == ProductStatus.DELETED)
            throw new ConflictException("商品已删除, 无法操作规格");
        return product;
    }

    /**
     * 校验规格存在并返回
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格实体
     */
    private @NotNull ProductSpec ensureSpec(@NotNull Long productId, @NotNull Long specId) {
        Product product = ensureProduct(productId);
        return productSpecRepository.findById(productId, specId)
                .orElseGet(() -> product.getSpecs().stream()
                        .filter(item -> Objects.equals(item.getId(), specId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalParamException("规格不存在")));
    }
}
