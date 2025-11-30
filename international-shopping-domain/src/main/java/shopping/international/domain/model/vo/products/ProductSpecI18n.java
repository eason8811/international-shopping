package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格多语言实体 ProductSpecI18n
 *
 * <p>承载规格在不同语言下的名称, 供管理侧批量维护与返回</p>
 */
@Getter
@ToString
public class ProductSpecI18n {
    /**
     * 语言代码
     */
    private final String locale;
    /**
     * 本地化规格名称
     */
    private final String specName;

    private ProductSpecI18n(String locale, String specName) {
        this.locale = locale;
        this.specName = specName;
    }

    /**
     * 构建规格多语言对象
     *
     * @param locale   语言代码
     * @param specName 本地化规格名称
     * @return 规格多语言对象
     * @throws IllegalParamException 当 locale 或 specName 为空时抛出 IllegalParamException
     */
    public static ProductSpecI18n of(String locale, String specName) {
        requireNotBlank(locale, "规格多语言的 locale 不能为空");
        requireNotBlank(specName, "规格多语言的名称不能为空");
        return new ProductSpecI18n(locale, specName);
    }
}
