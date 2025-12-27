package shopping.international.domain.model.entity.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 商品规格类别实体, 归属 SPU 聚合 ({@code product_spec}).
 *
 * <p>职责：维护规格编码/名称/类型、启停状态、多语言覆盖及规格值集合的不变式。</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class ProductSpec implements Verifiable {
    /**
     * 规格类别 ID (可为 null 表示未持久化)
     */
    private Long id;
    /**
     * 所属 SPU ID, 对应 {@code product_spec.product_id}
     */
    private Long productId;
    /**
     * 规格编码 (稳定唯一, 如 color/capacity)
     */
    private String specCode;
    /**
     * 规格名称 (默认语言)
     */
    private String specName;
    /**
     * 规格类型, 用于 UI 渲染/业务规则
     */
    private SpecType specType;
    /**
     * 是否必选 (每个 SKU 是否必须选择此规格)
     */
    private boolean required;
    /**
     * 排序值 (小在前)
     */
    private int sortOrder;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 多语言覆盖列表, locale 唯一
     */
    @NotNull
    private List<ProductSpecI18n> i18nList;
    /**
     * 规格值集合, valueCode 唯一
     */
    @NotNull
    private List<ProductSpecValue> values;

    /**
     * 私有构造函数
     *
     * @param id        规格 ID
     * @param productId 所属 SPU ID
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param specType  规格类型
     * @param required  是否必选
     * @param sortOrder 排序值
     * @param enabled   是否启用
     * @param i18nList  多语言列表
     * @param values    规格值集合
     */
    private ProductSpec(Long id, Long productId, String specCode, String specName, SpecType specType,
                        boolean required, int sortOrder, boolean enabled,
                        List<ProductSpecI18n> i18nList, List<ProductSpecValue> values) {
        requireNotNull(productId, "所属 SPU 不能为空");
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        requireNotNull(specType, "规格类型不能为空");
        this.id = id;
        this.productId = productId;
        this.specCode = specCode.strip();
        this.specName = specName.strip();
        this.specType = specType;
        this.required = required;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.i18nList = normalizeDistinctList(i18nList, ProductSpecI18n::validate, ProductSpecI18n::getLocale, "规格多语言 locale 不能重复");
        this.values = normalizeDistinctList(values, ProductSpecValue::validate, ProductSpecValue::getValueCode, "规格值编码不能重复");
        this.values.forEach(v -> {
            v.bindProductId(this.productId);
            if (this.id != null)
                v.bindSpecId(this.id);
        });
    }

    /**
     * 创建规格类别
     *
     * @param productId 所属 SPU ID, 必填
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param specType  规格类型
     * @param required  是否必选
     * @param sortOrder 排序值
     * @param enabled   是否启用
     * @param i18nList  多语言列表
     * @param values    规格值集合
     * @return 新建规格类别, id 为 null 表示未持久化
     */
    public static ProductSpec create(Long productId, String specCode, String specName, SpecType specType,
                                     boolean required, int sortOrder, boolean enabled,
                                     List<ProductSpecI18n> i18nList, List<ProductSpecValue> values) {
        return new ProductSpec(null, productId, specCode, specName, specType, required, sortOrder, enabled, i18nList, values);
    }

    /**
     * 从持久化层重建规格类别
     *
     * @param id        规格 ID
     * @param productId 所属 SPU ID
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param specType  规格类型
     * @param required  是否必选
     * @param sortOrder 排序值
     * @param enabled   是否启用
     * @param i18nList  多语言列表
     * @param values    规格值集合
     * @return 重建后的规格类别
     */
    public static ProductSpec reconstitute(Long id, Long productId, String specCode, String specName, SpecType specType,
                                           boolean required, int sortOrder, boolean enabled,
                                           List<ProductSpecI18n> i18nList, List<ProductSpecValue> values) {
        return new ProductSpec(id, productId, specCode, specName, specType, required, sortOrder, enabled, i18nList, values);
    }

    /**
     * 更新规格基本信息
     *
     * @param specCode  新规格代码, null 则保留
     * @param specName  新名称, null 时忽略
     * @param specType  新类型, null 时忽略
     * @param required  是否必选, null 时忽略
     * @param sortOrder 排序值, null 时忽略
     * @param enabled   启用状态, null 时忽略
     */
    public void update(String specCode, String specName, SpecType specType, Boolean required, Integer sortOrder, Boolean enabled) {
        if (specCode != null) {
            requireNotBlank(specCode, "规格代码不能为空");
            this.specCode = specCode.strip();
        }
        if (specName != null) {
            requireNotBlank(specName, "规格名称不能为空");
            this.specName = specName.strip();
        }
        if (specType != null)
            this.specType = specType;
        if (required != null)
            this.required = required;
        if (sortOrder != null)
            this.sortOrder = sortOrder;
        if (enabled != null)
            this.enabled = enabled;
    }

    /**
     * 替换多语言列表 (按 locale 去重)
     *
     * @param i18nList 新多语言列表
     */
    public void replaceI18n(List<ProductSpecI18n> i18nList) {
        this.i18nList = normalizeDistinctList(i18nList, ProductSpecI18n::validate, ProductSpecI18n::getLocale, "规格多语言 locale 不能重复");
    }

    /**
     * 新增多语言规格名 (locale 不可重复)
     *
     * @param i18n 多语言规格名
     */
    public void addI18n(ProductSpecI18n i18n) {
        requireNotNull(i18n, "规格多语言不能为空");
        i18n.validate();
        List<ProductSpecI18n> mutable = new ArrayList<>(i18nList);
        boolean exists = mutable.stream().anyMatch(item -> item.getLocale().equals(i18n.getLocale()));
        require(!exists, "规格多语言 locale 已存在: " + i18n.getLocale());
        mutable.add(i18n);
        replaceI18n(mutable);
    }

    /**
     * 批量更新规格多语言信息
     *
     * <p>此方法接收一个 {@link ProductSpecI18n} 对象列表, 并根据其中的 locale 信息更新当前对象中的对应多语言规格名. 如果传入的 locale 在当前对象中已存在,
     * 则更新其规格名称; 若不存在, 则添加新的多语言规格名. 更新过程中会对 locale 和 specName 进行非空校验</p>
     *
     * @param i18nList 待更新或新增的多语言规格名列表
     * @throws IllegalParamException 当 locale 或规格名称为空时抛出
     */
    public void updateI18nBatch(List<ProductSpecI18n> i18nList) {
        List<ProductSpecI18n> mutable = new ArrayList<>(this.i18nList);
        Map<String, ProductSpecI18n> exsistingI18nByLocaleMap = this.i18nList.stream()
                .collect(Collectors.toMap(ProductSpecI18n::getLocale, item -> item));
        for (ProductSpecI18n i18n : i18nList) {
            String normalizedLocale = normalizeLocale(i18n.getLocale());
            requireNotNull(normalizedLocale, "locale 不能为空");

            ProductSpecI18n existing = exsistingI18nByLocaleMap.get(normalizedLocale);
            if (existing == null) {
                i18n.validate();
                mutable.add(i18n);
                continue;
            }
            String mergedName = specName != null ? specName.strip() : existing.getSpecName();
            requireNotBlank(mergedName, "规格名称不能为空");

            ProductSpecI18n patched = ProductSpecI18n.of(normalizedLocale, mergedName);
            mutable.removeIf(item -> item.getLocale().equals(normalizedLocale));
            mutable.add(patched);
        }
        replaceI18n(mutable);
    }

    /**
     * 更新已存在的多语言规格名 (locale 必须存在, 为空不更新)
     *
     * @param locale   语言代码
     * @param specName 新名称, 为空则保留原值
     */
    public void updateI18n(String locale, String specName) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        List<ProductSpecI18n> mutable = new ArrayList<>(i18nList);
        ProductSpecI18n existing = mutable.stream()
                .filter(item -> item.getLocale().equals(normalizedLocale))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("规格多语言不存在: " + normalizedLocale));
        String mergedName = specName != null ? specName.strip() : existing.getSpecName();
        requireNotBlank(mergedName, "规格名称不能为空");

        ProductSpecI18n patched = ProductSpecI18n.of(normalizedLocale, mergedName);
        mutable.removeIf(item -> item.getLocale().equals(normalizedLocale));
        mutable.add(patched);
        replaceI18n(mutable);
    }

    /**
     * 替换规格值集合 (按 valueCode 去重)
     *
     * @param values 新规格值集合
     */
    public void replaceValues(List<ProductSpecValue> values) {
        this.values = normalizeDistinctList(values, ProductSpecValue::validate, ProductSpecValue::getValueCode, "规格值编码不能重复");
        this.values.forEach(v -> {
            v.bindProductId(this.productId);
            if (this.id != null)
                v.bindSpecId(this.id);
        });
    }

    /**
     * 更新指定编码的规格值信息
     *
     * @param valueId    规格值 ID
     * @param valueCode  规格值编码, 不可为空
     * @param valueName  新规格值名称, 可以为空
     * @param attributes 规格值附加属性, 键值对形式存储
     * @param sortOrder  排序值, 用于确定规格值显示顺序, 可以为 null
     * @param enabled    是否启用该规格值, 可以为 null
     * @param i18nList   多语言列表, 可空
     * @throws IllegalStateException 如果规格值不存在或规格值集合为 null 或 valueCode 为 null 或仅包含空白字符
     */
    public void updateValue(Long valueId, String valueCode, String valueName, Map<String, Object> attributes,
                            Integer sortOrder, Boolean enabled, List<ProductSpecValueI18n> i18nList) {
        requireNotNull(valueId, "规格值 ID 不能为空");
        ProductSpecValue existing = values.stream()
                .filter(v -> v.getId().equals(valueId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("规格值不存在: " + valueCode));
        for (ProductSpecValue value : values) {
            if (Objects.equals(value.getId(), valueId))
                continue;
            require(!value.getValueCode().equalsIgnoreCase(valueCode), "规格值编码已存在: " + valueCode);
            require(!value.getValueName().equalsIgnoreCase(valueName), "规格值名称已存在: " + valueName);
        }
        Map<String, List<String>> localeSpecValueI18nNameMap = values.stream()
                .map(ProductSpecValue::getI18nList)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        ProductSpecValueI18n::getLocale,
                        Collectors.mapping(ProductSpecValueI18n::getValueName, Collectors.toList())
                ));
        for (ProductSpecValueI18n i18n : i18nList) {
            List<String> specValueI18nName = localeSpecValueI18nNameMap.get(i18n.getLocale());
            if (specValueI18nName == null)
                continue;
            require(!specValueI18nName.contains(i18n.getValueName()),
                    i18n.getLocale() + " 语言的本地化的规格值名称已存在: " + i18n.getValueName());
        }
        existing.update(valueCode, valueName, attributes, sortOrder, enabled);
        existing.replaceI18n(i18nList);
    }

    /**
     * 附加一个规格值 (按编码判重)
     *
     * @param newValue 新规格值
     */
    public void addValue(ProductSpecValue newValue) {
        requireNotNull(newValue, "规格值不能为空");
        List<ProductSpecValue> mutable = new ArrayList<>(values);
        for (ProductSpecValue value : values) {
            require(!value.getValueCode().equalsIgnoreCase(newValue.getValueCode()), "规格值编码已存在: " + newValue.getValueCode());
            require(!value.getValueName().equalsIgnoreCase(newValue.getValueName()), "规格值名称已存在: " + newValue.getValueName());
        }
        Map<String, List<String>> localeSpecValueI18nNameMap = values.stream()
                .map(ProductSpecValue::getI18nList)
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        ProductSpecValueI18n::getLocale,
                        Collectors.mapping(ProductSpecValueI18n::getValueName, Collectors.toList())
                ));
        for (ProductSpecValueI18n i18n : newValue.getI18nList())
            require(!localeSpecValueI18nNameMap.get(i18n.getLocale()).contains(i18n.getValueName()),
                    i18n.getLocale() + " 语言的本地化的规格值名称已存在: " + i18n.getValueName());
        newValue.bindProductId(this.productId);
        if (this.id != null)
            newValue.bindSpecId(this.id);
        mutable.add(newValue);
        this.values = normalizeDistinctList(mutable, ProductSpecValue::validate, ProductSpecValue::getValueCode, "规格值编码不能重复");
    }

    /**
     * 绑定所属 SPU (幂等)
     *
     * @param productId SPU ID, 不可为空
     */
    public void bindProductId(Long productId) {
        requireNotNull(productId, "所属 SPU 不能为空");
        if (this.productId != null && !this.productId.equals(productId))
            throw new IllegalStateException("规格已绑定到其他 SPU, current=" + this.productId + ", new=" + productId);
        this.productId = productId;
    }

    /**
     * 为规格分配 ID (幂等)
     *
     * @param id 新 ID
     */
    public void assignId(Long id) {
        requireNotNull(id, "规格 ID 不能为空");
        if (this.id != null && !this.id.equals(id))
            throw new IllegalStateException("规格已存在 ID, 不允许覆盖, current=" + this.id + ", new=" + id);
        this.id = id;
        this.values.forEach(v -> v.bindSpecId(id));
    }

    /**
     * 校验规格实体
     */
    @Override
    public void validate() {
        requireNotNull(productId, "所属 SPU 不能为空");
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        requireNotNull(specType, "规格类型不能为空");
    }
}
