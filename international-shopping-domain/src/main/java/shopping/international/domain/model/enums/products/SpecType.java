package shopping.international.domain.model.enums.products;

/**
 * 规格类别类型
 * <ul>
 *     <li>{@code COLOR} - 颜色</li>
 *     <li>{@code SIZE} - 尺寸</li>
 *     <li>{@code CAPACITY} - 容量</li>
 *     <li>{@code MATERIAL} - 材质</li>
 *     <li>{@code OTHER} - 其他</li>
 * </ul>
 */
public enum SpecType {
    COLOR, SIZE, CAPACITY, MATERIAL, OTHER;

    /**
     * 解析字符串，大小写不敏感
     *
     * @param value 字符串值
     * @return 对应枚举，无法识别时默认 {@link #OTHER}
     */
    public static SpecType from(String value) {
        if (value == null)
            return OTHER;
        for (SpecType type : values()) {
            if (type.name().equalsIgnoreCase(value))
                return type;
        }
        return OTHER;
    }
}
