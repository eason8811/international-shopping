package shopping.international.domain.model.enums.user;

import shopping.international.types.exceptions.IllegalParamException;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 收货地址来源
 * <ul>
 *     <li>{@code MANUAL:} 人工</li>
 *     <li>{@code GOOGLE_AUTOCOMPLETE:} 自动填充</li>
 *     <li>{@code GOOGLE_MAP_PICK:} 地图选择</li>
 * </ul>
 */
public enum AddressSource {
    MANUAL,
    GOOGLE_AUTOCOMPLETE,
    GOOGLE_MAP_PICK;

    /**
     * 解析地址来源枚举
     *
     * @param raw 原始字符串
     * @return 枚举值
     */
    public static AddressSource parse(String raw) {
        requireNotBlank(raw, "addressSource 不能为空");
        try {
            return AddressSource.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalParamException("addressSource 不合法: " + raw);
        }
    }
}
