package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格值多语言载荷 ProductSpecValueI18nPayload
 */
@Data
public class ProductSpecValueI18nPayload {
    /**
     * 匹配 I18N 语言代码的正则
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
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
        requireNotBlank(locale, "规格值多语言的 locale 不能为空");
        locale = locale.strip();
        if (!LOCALE_PATTERN.matcher(locale).matches())
            throw new IllegalParamException("规格值多语言的 locale 格式不合法");

        requireNotBlank(valueName, "规格值名称不能为空");
        valueName = valueName.strip();
        if (valueName.length() > 64)
            throw new IllegalParamException("规格值名称长度不能超过 64 个字符");
    }
}
