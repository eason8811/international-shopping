package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 规格多语言载荷 ProductSpecI18nPayload
 */
@Data
public class ProductSpecI18nPayload implements Verifiable {
    /**
     * 语言代码, 例如 en-US
     */
    private String locale;
    /**
     * 规格名称
     */
    private String specName;

    /**
     * 校验并规范化多语言载荷
     *
     * @throws IllegalParamException 当必填字段缺失或格式非法时抛出 IllegalParamException
     */
    public void validate() {
        locale = normalizeNotNullField(locale, "规格多语言的 locale 不能为空", l -> l.length() <= 16, "locale 长度不能超过 16 个字符");
        locale = normalizeLocale(locale);
        specName = normalizeNotNullField(specName, "规格名称不能为空", s -> s.length() <= 64, "规格名称长度不能超过 64 个字符");
    }
}
