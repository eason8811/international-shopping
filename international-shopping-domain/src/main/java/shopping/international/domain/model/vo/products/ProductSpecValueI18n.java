package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格值多语言实体 ProductSpecValueI18n
 *
 * <p>用于维护规格值在不同语言下的展示名称</p>
 */
@Getter
@ToString
public class ProductSpecValueI18n {
    /**
     * 语言代码
     */
    private final String locale;
    /**
     * 本地化规格值名称
     */
    private final String valueName;

    private ProductSpecValueI18n(String locale, String valueName) {
        this.locale = locale;
        this.valueName = valueName;
    }

    /**
     * 构建规格值多语言对象
     *
     * @param locale    语言代码
     * @param valueName 本地化规格值名称
     * @return 规格值多语言对象
     * @throws IllegalParamException 当 locale 或 valueName 为空时抛出 IllegalParamException
     */
    public static ProductSpecValueI18n of(String locale, String valueName) {
        requireNotBlank(locale, "规格值多语言的 locale 不能为空");
        requireNotBlank(valueName, "规格值多语言的名称不能为空");
        return new ProductSpecValueI18n(locale, valueName);
    }
}
