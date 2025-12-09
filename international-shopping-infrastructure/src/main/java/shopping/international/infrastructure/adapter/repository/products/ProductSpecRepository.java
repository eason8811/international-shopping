package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductSpecRepository;
import shopping.international.domain.model.entity.products.ProductSpec;
import shopping.international.domain.model.entity.products.ProductSpecValue;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.infrastructure.dao.products.*;
import shopping.international.infrastructure.dao.products.po.*;
import shopping.international.types.exceptions.ConflictException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;

/**
 * 基于 MyBatis-Plus 的规格仓储实现
 *
 * <p>负责规格及规格值的组合加载、增删改, 并在写操作中处理 i18n 与属性序列化。</p>
 */
@Repository
@RequiredArgsConstructor
public class ProductSpecRepository implements IProductSpecRepository {

    /**
     * 规格主表 Mapper
     */
    private final ProductSpecMapper productSpecMapper;
    /**
     * 规格多语言 Mapper
     */
    private final ProductSpecI18nMapper productSpecI18nMapper;
    /**
     * 规格值主表 Mapper
     */
    private final ProductSpecValueMapper productSpecValueMapper;
    /**
     * 规格值多语言 Mapper
     */
    private final ProductSpecValueI18nMapper productSpecValueI18nMapper;
    /**
     * SKU-规格映射 Mapper, 用于删除保护
     */
    private final ProductSkuSpecMapper productSkuSpecMapper;
    /**
     * Jackson 对象映射器
     */
    private final ObjectMapper objectMapper;

    /**
     * 按 ID 查询规格
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格实体, 不存在返回空
     */
    @Override
    public @NotNull Optional<ProductSpec> findById(@NotNull Long productId, @NotNull Long specId) {
        ProductSpecPO po = productSpecMapper.selectAggregateById(productId, specId);
        if (po == null || po.getId() == null)
            return Optional.empty();
        return Optional.of(toSpec(po));
    }

    /**
     * 查询商品下的全部规格 (含规格值与多语言)
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    @Override
    public @NotNull List<ProductSpec> listByProductId(@NotNull Long productId) {
        List<ProductSpecPO> specs = productSpecMapper.selectAggregateByProductId(productId);
        if (specs == null || specs.isEmpty())
            return Collections.emptyList();
        return specs.stream().map(this::toSpec).toList();
    }

    /**
     * 新增规格及可选的规格值、多语言
     *
     * @param spec 规格实体, ID 为空
     * @return 携带持久化 ID 的规格实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSpec save(@NotNull ProductSpec spec) {
        ProductSpecPO po = ProductSpecPO.builder()
                .productId(spec.getProductId())
                .specCode(spec.getSpecCode())
                .specName(spec.getSpecName())
                .specType(spec.getSpecType().name())
                .isRequired(spec.isRequired())
                .sortOrder(spec.getSortOrder())
                .status(spec.isEnabled() ? "ENABLED" : "DISABLED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            productSpecMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("规格唯一约束冲突", e);
        }
        spec.assignId(po.getId());
        persistSpecI18n(spec.getId(), spec.getI18nList());
        persistValues(spec.getValues());
        return findById(spec.getProductId(), spec.getId())
                .orElseThrow(() -> new ConflictException("规格保存后回读失败"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSpec update(@NotNull ProductSpec spec, boolean replaceI18n) {
        LambdaUpdateWrapper<ProductSpecPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductSpecPO::getId, spec.getId())
                .set(ProductSpecPO::getSpecName, spec.getSpecName())
                .set(ProductSpecPO::getSpecType, spec.getSpecType().name())
                .set(ProductSpecPO::getIsRequired, spec.isRequired())
                .set(ProductSpecPO::getSortOrder, spec.getSortOrder())
                .set(ProductSpecPO::getStatus, spec.isEnabled() ? "ENABLED" : "DISABLED")
                .set(ProductSpecPO::getUpdatedAt, LocalDateTime.now());
        try {
            productSpecMapper.update(null, wrapper);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("规格唯一约束冲突", e);
        }
        if (replaceI18n)
            replaceSpecI18n(spec.getId(), spec.getI18nList());
        return findById(spec.getProductId(), spec.getId())
                .orElseThrow(() -> new ConflictException("规格更新后回读失败"));
    }

    /**
     * 删除规格及其规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpec(@NotNull Long productId, @NotNull Long specId) {
        List<ProductSpecValuePO> values = productSpecValueMapper.selectList(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getSpecId, specId));
        if (values != null && !values.isEmpty()) {
            List<Long> valueIds = values.stream().map(ProductSpecValuePO::getId).toList();
            productSpecValueI18nMapper.delete(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                    .in(ProductSpecValueI18nPO::getValueId, valueIds));
        }
        productSpecValueMapper.delete(new LambdaQueryWrapper<ProductSpecValuePO>().eq(ProductSpecValuePO::getSpecId, specId));
        productSpecI18nMapper.delete(new LambdaQueryWrapper<ProductSpecI18nPO>().eq(ProductSpecI18nPO::getSpecId, specId));
        int deleted = productSpecMapper.delete(new LambdaQueryWrapper<ProductSpecPO>()
                .eq(ProductSpecPO::getId, specId)
                .eq(ProductSpecPO::getProductId, productId));
        return deleted > 0;
    }

    /**
     * 查询指定规格下的规格值
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格值列表
     */
    @Override
    public @NotNull List<ProductSpecValue> listValues(@NotNull Long productId, @NotNull Long specId) {
        return findById(productId, specId)
                .map(ProductSpec::getValues)
                .orElse(Collections.emptyList());
    }

    /**
     * 新增规格值
     *
     * @param value 规格值实体, ID 为空
     * @return 保存后的规格值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSpecValue saveValue(@NotNull ProductSpecValue value) {
        ProductSpecValuePO po = ProductSpecValuePO.builder()
                .productId(value.getProductId())
                .specId(value.getSpecId())
                .valueCode(value.getValueCode())
                .valueName(value.getValueName())
                .attributes(writeAttributes(value.getAttributes()))
                .sortOrder(value.getSortOrder())
                .status(value.isEnabled() ? "ENABLED" : "DISABLED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            productSpecValueMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("规格值唯一约束冲突", e);
        }
        value.assignId(po.getId());
        persistValueI18n(value.getId(), value.getI18nList());
        return findById(value.getProductId(), value.getSpecId())
                .flatMap(spec -> spec.getValues().stream().filter(v -> Objects.equals(v.getId(), value.getId())).findFirst())
                .orElseThrow(() -> new ConflictException("规格值保存后回读失败"));
    }

    /**
     * 更新规格值
     *
     * @param value       规格值实体
     * @param replaceI18n 是否覆盖 i18n 表
     * @return 更新后的规格值
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductSpecValue updateValue(@NotNull ProductSpecValue value, boolean replaceI18n) {
        LambdaUpdateWrapper<ProductSpecValuePO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductSpecValuePO::getId, value.getId())
                .set(ProductSpecValuePO::getValueName, value.getValueName())
                .set(ProductSpecValuePO::getAttributes, writeAttributes(value.getAttributes()))
                .set(ProductSpecValuePO::getSortOrder, value.getSortOrder())
                .set(ProductSpecValuePO::getStatus, value.isEnabled() ? "ENABLED" : "DISABLED")
                .set(ProductSpecValuePO::getUpdatedAt, LocalDateTime.now());
        try {
            productSpecValueMapper.update(null, wrapper);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("规格值唯一约束冲突", e);
        }
        if (replaceI18n)
            replaceValueI18n(value.getId(), value.getI18nList());
        return findById(value.getProductId(), value.getSpecId())
                .flatMap(spec -> spec.getValues().stream().filter(v -> Objects.equals(v.getId(), value.getId())).findFirst())
                .orElseThrow(() -> new ConflictException("规格值更新后回读失败"));
    }

    /**
     * 删除规格值
     *
     * @param specId  规格 ID
     * @param valueId 规格值 ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteValue(@NotNull Long specId, @NotNull Long valueId) {
        productSpecValueI18nMapper.delete(new LambdaQueryWrapper<ProductSpecValueI18nPO>()
                .eq(ProductSpecValueI18nPO::getValueId, valueId));
        int deleted = productSpecValueMapper.delete(new LambdaQueryWrapper<ProductSpecValuePO>()
                .eq(ProductSpecValuePO::getId, valueId)
                .eq(ProductSpecValuePO::getSpecId, specId));
        return deleted > 0;
    }

    /**
     * 判断规格是否被 SKU 绑定
     *
     * @param specId 规格 ID
     * @return 若有绑定则返回 {@code true}
     */
    @Override
    public boolean hasSkuBindingsForSpec(@NotNull Long specId) {
        Long count = productSkuSpecMapper.selectCount(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .eq(ProductSkuSpecPO::getSpecId, specId));
        return count != null && count > 0;
    }

    /**
     * 判断规格值是否被 SKU 绑定
     *
     * @param valueId 规格值 ID
     * @return 若有绑定则返回 {@code true}
     */
    @Override
    public boolean hasSkuBindingsForValue(@NotNull Long valueId) {
        Long count = productSkuSpecMapper.selectCount(new LambdaQueryWrapper<ProductSkuSpecPO>()
                .eq(ProductSkuSpecPO::getValueId, valueId));
        return count != null && count > 0;
    }

    /**
     * 将规格 PO 聚合转换为领域实体
     *
     * @param po 规格持久化对象 (包含 i18n 与值集合)
     * @return 规格实体
     */
    private ProductSpec toSpec(@NotNull ProductSpecPO po) {
        List<ProductSpecI18n> i18n = toSpecI18n(po.getI18nList());
        List<ProductSpecValue> values = toSpecValues(po.getValues());
        return ProductSpec.reconstitute(
                po.getId(),
                po.getProductId(),
                po.getSpecCode(),
                po.getSpecName(),
                SpecType.from(po.getSpecType()),
                Boolean.TRUE.equals(po.getIsRequired()),
                po.getSortOrder() == null ? 0 : po.getSortOrder(),
                "ENABLED".equalsIgnoreCase(po.getStatus()),
                i18n,
                values
        );
    }

    /**
     * 将规格类别多语言持久化对象列表转换为领域实体列表
     *
     * @param pos 规格类别多语言持久化对象列表, 可以为 null 或空
     * @return 转换后的 {@link ProductSpecI18n} 对象列表, 如果输入为 null 或空, 则返回空列表
     */
    private List<ProductSpecI18n> toSpecI18n(@Nullable List<ProductSpecI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(item -> ProductSpecI18n.of(normalizeLocale(item.getLocale()), item.getSpecName()))
                .toList();
    }

    /**
     * 将规格值 PO 集合转换为领域实体
     *
     * @param pos 规格值持久化对象列表, 包含多语言
     * @return 规格值实体列表
     */
    private List<ProductSpecValue> toSpecValues(@Nullable List<ProductSpecValuePO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        List<ProductSpecValue> values = new ArrayList<>();
        for (ProductSpecValuePO po : pos) {
            List<ProductSpecValueI18n> i18nList = toValueI18n(po.getI18nList());
            Map<String, Object> attributes = readAttributes(po.getAttributes());
            ProductSpecValue value = ProductSpecValue.reconstitute(
                    po.getId(),
                    po.getProductId(),
                    po.getSpecId(),
                    po.getValueCode(),
                    po.getValueName(),
                    attributes,
                    po.getSortOrder() == null ? 0 : po.getSortOrder(),
                    "ENABLED".equalsIgnoreCase(po.getStatus()),
                    i18nList
            );
            values.add(value);
        }
        return values;
    }

    /**
     * 将规格值多语言持久化对象列表转换为领域实体列表
     *
     * @param pos 规格值多语言持久化对象列表, 可以为 null 或空
     * @return 转换后的 {@link ProductSpecValueI18n} 对象列表, 如果输入为 null 或空, 则返回空列表
     */
    private List<ProductSpecValueI18n> toValueI18n(@Nullable List<ProductSpecValueI18nPO> pos) {
        if (pos == null || pos.isEmpty())
            return Collections.emptyList();
        return pos.stream()
                .filter(Objects::nonNull)
                .map(item -> ProductSpecValueI18n.of(normalizeLocale(item.getLocale()), item.getValueName()))
                .toList();
    }

    /**
     * 覆盖规格多语言
     *
     * @param specId   规格 ID
     * @param i18nList 多语言列表
     */
    private void replaceSpecI18n(@NotNull Long specId, @NotNull List<ProductSpecI18n> i18nList) {
        productSpecI18nMapper.delete(new LambdaQueryWrapper<ProductSpecI18nPO>().eq(ProductSpecI18nPO::getSpecId, specId));
        persistSpecI18n(specId, i18nList);
    }

    /**
     * 覆盖规格值多语言
     *
     * @param valueId  规格值 ID
     * @param i18nList 多语言列表
     */
    private void replaceValueI18n(@NotNull Long valueId, @NotNull List<ProductSpecValueI18n> i18nList) {
        productSpecValueI18nMapper.delete(new LambdaQueryWrapper<ProductSpecValueI18nPO>().eq(ProductSpecValueI18nPO::getValueId, valueId));
        persistValueI18n(valueId, i18nList);
    }

    /**
     * 持久化规格值列表
     *
     * @param values 规格值集合
     */
    private void persistValues(@Nullable List<ProductSpecValue> values) {
        if (values == null || values.isEmpty())
            return;
        for (ProductSpecValue value : values) {
            persistSingleValue(value);
        }
    }

    /**
     * 插入单个规格值及其多语言
     *
     * @param value 规格值实体
     */
    private void persistSingleValue(@NotNull ProductSpecValue value) {
        ProductSpecValuePO po = ProductSpecValuePO.builder()
                .productId(value.getProductId())
                .specId(value.getSpecId())
                .valueCode(value.getValueCode())
                .valueName(value.getValueName())
                .attributes(writeAttributes(value.getAttributes()))
                .sortOrder(value.getSortOrder())
                .status(value.isEnabled() ? "ENABLED" : "DISABLED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            productSpecValueMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("规格值唯一约束冲突", e);
        }
        value.assignId(po.getId());
        persistValueI18n(po.getId(), value.getI18nList());
    }

    /**
     * 插入规格多语言
     *
     * @param specId   规格 ID
     * @param i18nList 多语言列表
     */
    private void persistSpecI18n(@NotNull Long specId, @NotNull List<ProductSpecI18n> i18nList) {
        if (i18nList.isEmpty())
            return;
        for (ProductSpecI18n i18n : i18nList) {
            ProductSpecI18nPO po = ProductSpecI18nPO.builder()
                    .specId(specId)
                    .locale(i18n.getLocale())
                    .specName(i18n.getSpecName())
                    .createdAt(LocalDateTime.now())
                    .build();
            productSpecI18nMapper.insert(po);
        }
    }

    /**
     * 插入规格值多语言
     *
     * @param valueId  规格值 ID
     * @param i18nList 多语言列表
     */
    private void persistValueI18n(@NotNull Long valueId, @NotNull List<ProductSpecValueI18n> i18nList) {
        if (i18nList.isEmpty())
            return;
        for (ProductSpecValueI18n i18n : i18nList) {
            ProductSpecValueI18nPO po = ProductSpecValueI18nPO.builder()
                    .valueId(valueId)
                    .locale(i18n.getLocale())
                    .valueName(i18n.getValueName())
                    .createdAt(LocalDateTime.now())
                    .build();
            productSpecValueI18nMapper.insert(po);
        }
    }

    /**
     * 反序列化属性 JSON
     *
     * @param json JSON 字符串
     * @return 属性映射
     */
    private Map<String, Object> readAttributes(@Nullable String json) {
        if (json == null || json.isBlank())
            return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 序列化属性 JSON
     *
     * @param attributes 属性映射
     * @return JSON 字符串
     */
    private String writeAttributes(@NotNull Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("规格属性序列化失败", e);
        }
    }
}
