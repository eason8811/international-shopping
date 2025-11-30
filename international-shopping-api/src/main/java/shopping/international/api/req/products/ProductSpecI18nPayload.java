package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 规格多语言载荷 ProductSpecI18nPayload
 */
@Data
public class ProductSpecI18nPayload {
    /**
     * 匹配 I18N 语言代码的正则, 允许连字符或下划线分隔的区域后缀
     */
    private static final Pattern LOCALE_PATTERN = Pattern.compile("^[A-Za-z0-9]{2,8}([-_][A-Za-z0-9]{2,8})*$");
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
        if (!LOCALE_PATTERN.matcher(locale).matches())
            throw new IllegalParamException("规格多语言的 locale 格式不合法");

        requireNotBlank(specName, "规格名称不能为空");
        specName = specName.strip();
        if (specName.length() > 64)
            throw new IllegalParamException("规格名称长度不能超过 64 个字符");
    }
}
