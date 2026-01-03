package shopping.international.types.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 汇率数据源枚举
 *
 * <p>用于落库 {@code fx_rate_latest.provider}/{@code fx_rate.provider} 与价格派生元数据 {@code product_price.fx_provider}</p>
 */
public enum FxRateProvider {
    /**
     * ExchangeRate-API 免费镜像 (open.er-api.com)
     */
    OPEN_ER_API("OPEN_ER_API");

    /**
     * 数据源编码 (用于落库, 固定大写下划线风格)
     */
    private final String code;

    FxRateProvider(String code) {
        this.code = code;
    }

    /**
     * 获取数据源编码
     *
     * @return 数据源编码
     */
    public @NotNull String code() {
        return code;
    }

    /**
     * 从字符串解析数据源枚举
     *
     * @param raw 原始字符串
     * @return 枚举或 null
     */
    public static @Nullable FxRateProvider ofNullable(@Nullable String raw) {
        if (raw == null || raw.isBlank())
            return null;
        String normalized = raw.strip().toUpperCase(Locale.ROOT);
        for (FxRateProvider p : values())
            if (p.code.equals(normalized))
                return p;
        return null;
    }

    /**
     * 从字符串解析数据源枚举 (非空)
     *
     * @param raw 原始字符串
     * @return 枚举
     */
    public static @NotNull FxRateProvider of(@NotNull String raw) {
        FxRateProvider p = ofNullable(raw);
        requireNotNull(p, "fx_provider 不合法: " + raw);
        return p;
    }
}

