package shopping.international.domain.model.enums.products;

/**
 * 商品列表排序方式
 */
public enum ProductSort {
    /**
     * 最新（更新时间倒序）
     */
    LATEST,
    /**
     * 销量倒序
     */
    SALES_DESC,
    /**
     * 价格升序
     */
    PRICE_ASC,
    /**
     * 价格降序
     */
    PRICE_DESC;

    /**
     * 通过字符串解析排序方式
     *
     * @param value 输入字符串
     * @return 枚举，无法识别时默认 {@link #LATEST}
     */
    public static ProductSort from(String value) {
        if (value == null || value.isBlank())
            return LATEST;
        for (ProductSort sort : values()) {
            if (sort.name().equalsIgnoreCase(value))
                return sort;
        }
        return LATEST;
    }
}
