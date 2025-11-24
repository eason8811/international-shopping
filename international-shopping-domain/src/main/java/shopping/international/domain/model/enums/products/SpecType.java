package shopping.international.domain.model.enums.products;

/**
 * 规格类别类型
 */
public enum SpecType {
    /**
     * 颜色
     */
    COLOR,
    /**
     * 尺寸
     */
    SIZE,
    /**
     * 容量
     */
    CAPACITY,
    /**
     * 材质
     */
    MATERIAL,
    /**
     * 其他
     */
    OTHER;

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
