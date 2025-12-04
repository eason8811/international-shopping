package shopping.international.domain.model.entity.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecI18n;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.List;

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
    private List<ProductSpecI18n> i18nList;
    /**
     * 规格值集合, valueCode 唯一
     */
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
        if (this.values != null) {
            this.values.forEach(v -> {
                v.bindProductId(this.productId);
                if (this.id != null)
                    v.bindSpecId(this.id);
            });
        }
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
     * @param specName  新名称, null 时忽略
     * @param specType  新类型, null 时忽略
     * @param required  是否必选, null 时忽略
     * @param sortOrder 排序值, null 时忽略
     * @param enabled   启用状态, null 时忽略
     */
    public void update(String specName, SpecType specType, Boolean required, Integer sortOrder, Boolean enabled) {
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
     * 替换规格值集合 (按 valueCode 去重)
     *
     * @param values 新规格值集合
     */
    public void replaceValues(List<ProductSpecValue> values) {
        this.values = normalizeDistinctList(values, ProductSpecValue::validate, ProductSpecValue::getValueCode, "规格值编码不能重复");
        if (this.values != null) {
            this.values.forEach(v -> {
                v.bindProductId(this.productId);
                if (this.id != null)
                    v.bindSpecId(this.id);
            });
        }
    }

    /**
     * 附加一个规格值 (按编码判重)
     *
     * @param newValue 新规格值
     */
    public void addValue(ProductSpecValue newValue) {
        requireNotNull(newValue, "规格值不能为空");
        if (values == null)
            values = new ArrayList<>();
        boolean exists = values.stream().anyMatch(v -> v.getValueCode().equals(newValue.getValueCode()));
        if (exists)
            throw new IllegalStateException("规格值编码重复: " + newValue.getValueCode());
        newValue.bindProductId(this.productId);
        if (this.id != null)
            newValue.bindSpecId(this.id);
        values.add(newValue);
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
        if (this.values != null)
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
