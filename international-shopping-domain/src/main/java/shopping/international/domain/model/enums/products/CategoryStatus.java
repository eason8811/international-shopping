package shopping.international.domain.model.enums.products;

/**
 * 商品分类状态
 */
public enum CategoryStatus {
    /**
     * 启用
     */
    ENABLED,
    /**
     * 禁用
     */
    DISABLED;

    /**
     * 通过字符串解析状态，大小写不敏感
     *
     * @param value 枚举字符串
     * @return 匹配到的 {@link CategoryStatus}，无法匹配时默认 {@link #DISABLED}
     */
    public static CategoryStatus from(String value) {
        if (value == null)
            return DISABLED;
        if ("enabled".equalsIgnoreCase(value))
            return ENABLED;
        if ("disabled".equalsIgnoreCase(value))
            return DISABLED;
        return DISABLED;
    }
}
