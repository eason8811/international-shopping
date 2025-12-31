package shopping.international.domain.model.vo.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

/**
 * 币种配置快照 (来自 currency 表)
 *
 * @param code            币种代码
 * @param minorUnit       币种最小单位
 * @param roundingMode    币种四舍五入模式
 * @param cashRoundingInc 币种现金四舍五入增量
 * @param enabled         币种是否启用
 */
public record CurrencyProfile(@NotNull String code,
                              @Nullable Integer minorUnit,
                              @Nullable String roundingMode,
                              @Nullable BigDecimal cashRoundingInc,
                              @Nullable Boolean enabled) {
}

