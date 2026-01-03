package shopping.international.domain.model.enums.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * SKU 价格来源 (product_price.price_source)
 */
public enum ProductPriceSource {
    /**
     * 人工维护
     */
    MANUAL,
    /**
     * 基于 FX 派生
     */
    FX_AUTO;

    /**
     * 解析字符串为枚举, 为空返回 null
     */
    public static @Nullable ProductPriceSource parseNullable(@Nullable String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return ProductPriceSource.valueOf(raw.strip());
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 解析字符串为枚举, 不合法则抛异常
     */
    public static @NotNull ProductPriceSource parseOrThrow(@Nullable String raw) {
        ProductPriceSource v = parseNullable(raw);
        requireNotNull(v, "price_source 不合法: " + raw);
        return v;
    }
}

