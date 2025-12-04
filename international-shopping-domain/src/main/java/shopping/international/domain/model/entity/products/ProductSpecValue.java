package shopping.international.domain.model.entity.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.domain.model.vo.products.ProductSpecValueI18n;
import shopping.international.types.utils.Verifiable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格值实体, 归属 {@link shopping.international.domain.model.entity.products.ProductSpec} ({@code product_spec_value}).
 *
 * <p>职责：维护规格值编码/名称、排序、启停、多语言覆盖及附加属性。</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class ProductSpecValue implements Verifiable {
    /**
     * 规格值 ID (可为空表示未持久化)
     */
    private Long id;
    /**
     * 所属 SPU ID (冗余校验)
     */
    private Long productId;
    /**
     * 所属规格类别 ID
     */
    private Long specId;
    /**
     * 规格值编码 (稳定唯一)
     */
    private String valueCode;
    /**
     * 规格值名称
     */
    private String valueName;
    /**
     * 附加属性 (如颜色 hex、展示图等)
     */
    private Map<String, Object> attributes;
    /**
     * 排序值 (小在前)
     */
    private int sortOrder;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 多语言列表, locale 唯一
     */
    private List<ProductSpecValueI18n> i18nList;

    /**
     * 私有构造函数
     *
     * @param id         规格值 ID
     * @param productId  所属 SPU ID
     * @param specId     所属规格类别 ID
     * @param valueCode  值编码
     * @param valueName  值名称
     * @param attributes 附加属性
     * @param sortOrder  排序
     * @param enabled    是否启用
     * @param i18nList   多语言列表
     */
    private ProductSpecValue(Long id, Long productId, Long specId, String valueCode, String valueName,
                             Map<String, Object> attributes, int sortOrder, boolean enabled,
                             List<ProductSpecValueI18n> i18nList) {
        requireNotNull(productId, "所属 SPU 不能为空");
        requireNotBlank(valueCode, "规格值编码不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
        this.id = id;
        this.productId = productId;
        this.specId = specId;
        this.valueCode = valueCode.strip();
        this.valueName = valueName.strip();
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.i18nList = normalizeDistinctList(i18nList, ProductSpecValueI18n::validate, ProductSpecValueI18n::getLocale, "规格值多语言 locale 不能重复");
    }

    /**
     * 创建规格值
     *
     * @param productId  所属 SPU ID
     * @param specId     所属规格 ID, 可为空 (由规格分配后绑定)
     * @param valueCode  值编码
     * @param valueName  值名称
     * @param attributes 附加属性
     * @param sortOrder  排序
     * @param enabled    是否启用
     * @param i18nList   多语言列表
     * @return 新规格值
     */
    public static ProductSpecValue create(Long productId, Long specId, String valueCode, String valueName,
                                          Map<String, Object> attributes, int sortOrder, boolean enabled,
                                          List<ProductSpecValueI18n> i18nList) {
        return new ProductSpecValue(null, productId, specId, valueCode, valueName, attributes, sortOrder, enabled, i18nList);
    }

    /**
     * 重建规格值
     *
     * @param id         规格值 ID
     * @param productId  所属 SPU ID
     * @param specId     所属规格 ID
     * @param valueCode  值编码
     * @param valueName  值名称
     * @param attributes 附加属性
     * @param sortOrder  排序
     * @param enabled    是否启用
     * @param i18nList   多语言列表
     * @return 重建后的规格值
     */
    public static ProductSpecValue reconstitute(Long id, Long productId, Long specId, String valueCode, String valueName,
                                                Map<String, Object> attributes, int sortOrder, boolean enabled,
                                                List<ProductSpecValueI18n> i18nList) {
        return new ProductSpecValue(id, productId, specId, valueCode, valueName, attributes, sortOrder, enabled, i18nList);
    }

    /**
     * 更新规格值基础信息
     *
     * @param valueName  新名称, null 时忽略
     * @param attributes 新属性, null 时忽略
     * @param sortOrder  新排序, null 时忽略
     * @param enabled    新启用状态, null 时忽略
     */
    public void update(String valueName, Map<String, Object> attributes, Integer sortOrder, Boolean enabled) {
        if (valueName != null) {
            requireNotBlank(valueName, "规格值名称不能为空");
            this.valueName = valueName.strip();
        }
        if (attributes != null)
            this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        if (sortOrder != null)
            this.sortOrder = sortOrder;
        if (enabled != null)
            this.enabled = enabled;
    }

    /**
     * 替换多语言列表
     *
     * @param i18nList 新多语言列表
     */
    public void replaceI18n(List<ProductSpecValueI18n> i18nList) {
        this.i18nList = normalizeDistinctList(i18nList, ProductSpecValueI18n::validate, ProductSpecValueI18n::getLocale, "规格值多语言 locale 不能重复");
    }

    /**
     * 绑定所属规格 (幂等)
     *
     * @param specId 规格 ID
     */
    public void bindSpecId(Long specId) {
        requireNotNull(specId, "规格 ID 不能为空");
        if (this.specId != null && !this.specId.equals(specId))
            throw new IllegalStateException("规格值已绑定到其他规格, current=" + this.specId + ", new=" + specId);
        this.specId = specId;
    }

    /**
     * 绑定所属 SPU (幂等)
     *
     * @param productId SPU ID
     */
    public void bindProductId(Long productId) {
        requireNotNull(productId, "所属 SPU 不能为空");
        if (this.productId != null && !this.productId.equals(productId))
            throw new IllegalStateException("规格值已绑定到其他 SPU, current=" + this.productId + ", new=" + productId);
        this.productId = productId;
    }

    /**
     * 为规格值分配 ID (幂等)
     *
     * @param id 新 ID
     */
    public void assignId(Long id) {
        requireNotNull(id, "规格值 ID 不能为空");
        if (this.id != null && !this.id.equals(id))
            throw new IllegalStateException("规格值已存在 ID, 不允许覆盖, current=" + this.id + ", new=" + id);
        this.id = id;
    }

    /**
     * 校验规格值实体
     */
    @Override
    public void validate() {
        requireNotNull(productId, "所属 SPU 不能为空");
        requireNotBlank(valueCode, "规格值编码不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
    }
}
