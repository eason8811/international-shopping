package shopping.international.domain.model.enums.products;

/**
 * 商品状态
 * <ul>
 *     <li>{@code DRAFT} - 草稿</li>
 *     <li>{@code ON_SALE} - 上架</li>
 *     <li>{@code OFF_SHELF} - 下架</li>
 *     <li>{@code DELETED} - 已删除</li>
 * </ul>
 */
public enum ProductStatus {
    DRAFT, ON_SALE, OFF_SHELF, DELETED;

    /**
     * 解析字符串为枚举值，大小写不敏感
     *
     * @param value 字符串值
     * @return 对应的枚举，无法识别时默认 {@link #DRAFT}
     */
    public static ProductStatus from(String value) {
        if (value == null)
            return DRAFT;
        for (ProductStatus status : values()) {
            if (status.name().equalsIgnoreCase(value))
                return status;
        }
        return DRAFT;
    }
}
