package shopping.international.domain.model.enums.products;

/**
 * 商品规格类型 (单/多规格)
 * <ul>
 *     <li>{@link #SINGLE} - 单规格</li>
 *     <li>{@link #VARIANT} - 多规格</li>
 * </ul>
 */
public enum SkuType {
    SINGLE, VARIANT;

    /**
     * 解析字符串，大小写不敏感
     *
     * @param value 输入值
     * @return 匹配枚举，无法识别时默认 {@link #SINGLE}
     */
    public static SkuType from(String value) {
        if (value == null)
            return SINGLE;
        for (SkuType type : values()) {
            if (type.name().equalsIgnoreCase(value))
                return type;
        }
        return SINGLE;
    }
}
