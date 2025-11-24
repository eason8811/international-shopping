package shopping.international.domain.model.enums.products;

/**
 * 商品状态
 */
public enum ProductStatus {
    /**
     * 草稿
     */
    DRAFT,
    /**
     * 上架
     */
    ON_SALE,
    /**
     * 下架
     */
    OFF_SHELF,
    /**
     * 已删除
     */
    DELETED;

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
