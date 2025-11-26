package shopping.international.domain.model.enums.products;

/**
 * SKU 状态
 * <ul>
 *     <li>{@code ENABLED} - 启用</li>
 *     <li>{@code DISABLED} - 禁用</li>
 * </ul>
 */
public enum SkuStatus {
    ENABLED, DISABLED;

    /**
     * 解析状态，大小写不敏感
     *
     * @param value 字符串值
     * @return 匹配枚举，无法识别时默认 {@link #DISABLED}
     */
    public static SkuStatus from(String value) {
        if (value == null)
            return DISABLED;
        if ("enabled".equalsIgnoreCase(value))
            return ENABLED;
        if ("disabled".equalsIgnoreCase(value))
            return DISABLED;
        return DISABLED;
    }
}
