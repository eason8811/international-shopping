package shopping.international.domain.model.entity.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.products.SpecType;
import shopping.international.domain.model.vo.products.ProductSpecValue;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 商品规格类别
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ProductSpec {
    /**
     * 规格 ID
     */
    private Long id;
    /**
     * 所属商品 ID
     */
    private Long productId;
    /**
     * 编码
     */
    private String specCode;
    /**
     * 名称
     */
    private String specName;
    /**
     * 类型
     */
    private SpecType specType;
    /**
     * 是否必选
     */
    private boolean required;
    /**
     * 本地化名称
     */
    private String i18nName;
    /**
     * 规格值
     */
    private List<ProductSpecValue> values = new ArrayList<>();

    private ProductSpec() {
    }

    /**
     * 重建规格
     *
     * @param id        规格ID
     * @param productId 商品ID
     * @param specCode  规格编码
     * @param specName  规格名称
     * @param specType  规格类型
     * @param required  是否必选
     * @return 规格实体
     */
    public static ProductSpec reconstitute(Long id,
                                           Long productId,
                                           @NotNull String specCode,
                                           @NotNull String specName,
                                           SpecType specType,
                                           Boolean required) {
        if (id == null)
            throw new IllegalParamException("规格 ID 不能为空");
        if (productId == null)
            throw new IllegalParamException("规格所属商品 ID 不能为空");
        requireNotBlank(specCode, "规格编码不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        ProductSpec spec = new ProductSpec();
        spec.id = id;
        spec.productId = productId;
        spec.specCode = specCode;
        spec.specName = specName;
        spec.specType = specType == null ? SpecType.OTHER : specType;
        spec.required = Boolean.TRUE.equals(required);
        return spec;
    }

    /**
     * 设置本地化名称
     *
     * @param i18nName 名称
     */
    public void applyI18n(String i18nName) {
        if (i18nName != null && !i18nName.isBlank())
            this.i18nName = i18nName;
    }

    /**
     * 绑定规格值
     *
     * @param values 值列表
     */
    public void attachValues(List<ProductSpecValue> values) {
        if (values != null)
            this.values = new ArrayList<>(values);
    }

    /**
     * 不可变的规格值列表
     *
     * @return 规格值
     */
    public List<ProductSpecValue> getValues() {
        return Collections.unmodifiableList(values);
    }
}
