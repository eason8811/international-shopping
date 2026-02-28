package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 补发原因枚举, 对齐表 `aftersales_reship.reason_code`
 * <ul>
 *     <li>{@code LOST:} 丢件</li>
 *     <li>{@code DAMAGED:} 破损</li>
 *     <li>{@code OTHER:} 其他</li>
 * </ul>
 */
public enum ReshipReasonCode {
    /**
     * 丢件
     */
    LOST,
    /**
     * 破损
     */
    DAMAGED,
    /**
     * 其他
     */
    OTHER;

    /**
     * 将字符串转换为补发原因枚举
     *
     * @param value 原始字符串值
     * @return 对应的补发原因枚举
     */
    public static ReshipReasonCode fromValue(String value) {
        requireNotBlank(value, "reasonCode 不能为空");
        return ReshipReasonCode.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
