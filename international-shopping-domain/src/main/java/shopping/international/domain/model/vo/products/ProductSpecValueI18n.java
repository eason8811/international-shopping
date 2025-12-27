package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格值多语言值对象, 对应表 {@code product_spec_value_i18n}.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "locale")
public class ProductSpecValueI18n implements Verifiable {
    /**
     * 语言代码
     */
    private final String locale;
    /**
     * 多语言规格值名称
     */
    private final String valueName;

    /**
     * 构造函数
     *
     * @param locale    语言代码
     * @param valueName 规格值名称
     */
    private ProductSpecValueI18n(String locale, String valueName) {
        this.locale = locale;
        this.valueName = valueName;
    }

    /**
     * 创建规格值多语言值对象
     *
     * @param locale    语言代码, 必填
     * @param valueName 规格值名称, 必填
     * @return 规范化后的 {@link ProductSpecValueI18n}
     */
    public static ProductSpecValueI18n of(String locale, String valueName) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotNull(normalizedLocale, "locale 不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
        return new ProductSpecValueI18n(normalizedLocale, valueName.strip());
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotNull(locale, "locale 不能为空");
        requireNotBlank(valueName, "规格值名称不能为空");
    }
}
