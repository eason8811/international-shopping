package shopping.international.domain.model.vo.products;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeLocale;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格类别多语言值对象, 对应表 {@code product_spec_i18n}.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "locale")
public class ProductSpecI18n implements Verifiable {
    /**
     * 语言代码
     */
    private final String locale;
    /**
     * 本地化规格名称
     */
    private final String specName;

    /**
     * 构造函数
     *
     * @param locale   语言代码
     * @param specName 多语言规格名
     */
    private ProductSpecI18n(String locale, String specName) {
        this.locale = locale;
        this.specName = specName;
    }

    /**
     * 创建规格多语言值对象
     *
     * @param locale   语言代码, 必填
     * @param specName 规格名称, 必填
     * @return 规范化后的 {@link ProductSpecI18n}
     */
    public static ProductSpecI18n of(String locale, String specName) {
        String normalizedLocale = normalizeLocale(locale);
        requireNotBlank(normalizedLocale, "locale 不能为空");
        requireNotBlank(specName, "规格名称不能为空");
        return new ProductSpecI18n(normalizedLocale, specName.strip());
    }

    /**
     * 校验当前值对象
     */
    @Override
    public void validate() {
        requireNotBlank(locale, "locale 不能为空");
        requireNotBlank(specName, "规格名称不能为空");
    }
}
