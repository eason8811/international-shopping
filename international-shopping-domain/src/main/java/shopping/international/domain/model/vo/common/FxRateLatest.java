package shopping.international.domain.model.vo.common;

import org.jetbrains.annotations.NotNull;
import shopping.international.types.enums.FxRateProvider;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 最新汇率快照值对象 (对应表 fx_rate_latest)
 */
public record FxRateLatest(@NotNull String baseCode,
                           @NotNull String quoteCode,
                           @NotNull BigDecimal rate,
                           @NotNull LocalDateTime asOf,
                           @NotNull FxRateProvider provider) implements Verifiable {

    /**
     * 创建最新汇率快照
     *
     * @param baseCode  基准币种 (如 USD)
     * @param quoteCode 报价币种 (如 EUR)
     * @param rate      汇率, 表示 1 base = rate quote
     * @param asOf      采样时间
     * @param provider  数据源
     * @return 值对象
     */
    public static @NotNull FxRateLatest of(@NotNull String baseCode,
                                           @NotNull String quoteCode,
                                           @NotNull BigDecimal rate,
                                           @NotNull LocalDateTime asOf,
                                           @NotNull FxRateProvider provider) {
        FxRateLatest vo = new FxRateLatest(baseCode, quoteCode, rate, asOf, provider);
        vo.validate();
        return vo;
    }

    /**
     * 判断该汇率是否仍在有效期内
     *
     * @param clock  时钟
     * @param maxAge 允许的最大延迟
     * @return 是否有效
     */
    public boolean isFresh(@NotNull Clock clock, @NotNull Duration maxAge) {
        requireNotNull(clock, "clock 不能为空");
        requireNotNull(maxAge, "maxAge 不能为空");
        LocalDateTime now = LocalDateTime.now(clock);
        return !asOf.isAfter(now) && Duration.between(asOf, now).compareTo(maxAge) <= 0;
    }

    /**
     * 校验字段合法性
     */
    @Override
    public void validate() {
        String base = normalizeCurrency(baseCode);
        String quote = normalizeCurrency(quoteCode);
        requireNotNull(base, "baseCode 不能为空");
        requireNotNull(quote, "quoteCode 不能为空");
        require(!base.equals(quote), "baseCode 与 quoteCode 不能相同");
        requireNotNull(rate, "rate 不能为空");
        require(rate.compareTo(BigDecimal.ZERO) > 0, "rate 必须大于 0");
        requireNotNull(asOf, "asOf 不能为空");
        requireNotNull(provider, "provider 不能为空");
    }
}

