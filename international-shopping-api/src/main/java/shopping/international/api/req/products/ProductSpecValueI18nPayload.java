package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格值多语言载荷 ProductSpecValueI18nPayload
 */
@Data
public class ProductSpecValueI18nPayload implements Verifiable {
    /**
     * 语言代码, 例如 en-US
     */
    private String locale;
    /**
     * 本地化的规格值名称
     */
    private String valueName;

    /**
     * 校验并规范化规格值多语言字段
     *
     * @throws IllegalParamException 当 locale 或 valueName 非法时抛出 IllegalParamException
     */
    public void validate() {
        locale = normalizeNotNullField(locale, "规格多语言的 locale 不能为空", l -> l.length() <= 16, "locale 长度不能超过 16 个字符");
        locale = normalizeLocale(locale);
        valueName = normalizeNotNullField(valueName, "规格值名称不能为空", v -> v.length() <= 64, "规格值名称长度不能超过 64 个字符");
    }
}
