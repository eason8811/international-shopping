package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品规格值
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ProductSpecValue {
    /**
     * 主键
     */
    private Long id;
    /**
     * 所属商品 ID
     */
    private Long productId;
    /**
     * 所属规格 ID
     */
    private Long specId;
    /**
     * 值编码
     */
    private String valueCode;
    /**
     * 值名称
     */
    private String valueName;
    /**
     * 扩展属性
     */
    private Map<String, Object> attributes;
    /**
     * i18n 名称
     */
    private String i18nName;
    /**
     * 排序权重
     */
    private int sortOrder;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 多语言列表
     */
    private List<ProductSpecValueI18n> i18nList = new ArrayList<>();

    private ProductSpecValue() {
    }

    /**
     * 重建规格值
     *
     * @param id        主键
     * @param productId 所属商品 ID
     * @param specId    规格ID
     * @param valueCode 编码
     * @param valueName 名称
     * @param attributes 扩展属性
     * @return 规格值
     */
    public static ProductSpecValue reconstitute(Long id,
                                                Long productId,
                                                Long specId,
                                                @NotNull String valueCode,
                                                @NotNull String valueName,
                                                Map<String, Object> attributes) {
        return reconstitute(id, productId, specId, valueCode, valueName, attributes, 0, true);
    }

    /**
     * 重建规格值
     *
     * @param id        主键
     * @param productId 所属商品 ID
     * @param specId    规格ID
     * @param valueCode 编码
     * @param valueName 名称
     * @param attributes 扩展属性
     * @param sortOrder 排序
     * @param enabled   是否启用
     * @return 规格值
     */
    public static ProductSpecValue reconstitute(Long id,
                                                Long productId,
                                                Long specId,
                                                @NotNull String valueCode,
                                                @NotNull String valueName,
                                                Map<String, Object> attributes,
                                                Integer sortOrder,
                                                Boolean enabled) {
        if (id == null)
            throw new IllegalParamException("规格值 ID 不能为空");
        if (specId == null)
            throw new IllegalParamException("规格类别 ID 不能为空");
        if (productId == null)
            throw new IllegalParamException("规格值所属商品 ID 不能为空");
        requireNotBlank(valueCode, "规格值编码不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
        ProductSpecValue value = new ProductSpecValue();
        value.id = id;
        value.productId = productId;
        value.specId = specId;
        value.valueCode = valueCode;
        value.valueName = valueName;
        value.attributes = attributes == null ? Collections.emptyMap() : attributes;
        value.sortOrder = sortOrder == null ? 0 : sortOrder;
        value.enabled = !Boolean.FALSE.equals(enabled);
        return value;
    }

    /**
     * 应用本地化名称
     *
     * @param localizedName 名称
     */
    public void applyI18n(String localizedName) {
        if (localizedName != null && !localizedName.isBlank())
            this.i18nName = localizedName;
    }

    /**
     * 绑定多语言列表
     *
     * @param i18nList 多语言列表
     */
    public void attachI18nList(List<ProductSpecValueI18n> i18nList) {
        if (i18nList != null)
            this.i18nList = new ArrayList<>(i18nList);
    }

    /**
     * 获取不可变的多语言列表
     *
     * @return 多语言列表
     */
    public List<ProductSpecValueI18n> getI18nList() {
        return Collections.unmodifiableList(i18nList);
    }
}
