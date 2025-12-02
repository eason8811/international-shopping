package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.FieldValidateUtils;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格多语言载荷 ProductSpecI18nPayload
 */
@Data
public class ProductSpecI18nPayload {
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
        requireNotBlank(locale, "规格多语言的 locale 不能为空");
        locale = locale.strip();
        if (!FieldValidateUtils.LOCALE_PATTERN.matcher(locale).matches())
            throw new IllegalParamException("规格多语言的 locale 格式不合法");

        requireNotBlank(specName, "规格名称不能为空");
        specName = specName.strip();
        if (specName.length() > 64)
            throw new IllegalParamException("规格名称长度不能超过 64 个字符");
    }
}
