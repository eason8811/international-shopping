package shopping.international.infrastructure.adapter.port.common;

import lombok.RequiredArgsConstructor;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import shopping.international.domain.adapter.port.common.IExchangeRatePort;
import shopping.international.domain.model.vo.common.FxRateLatest;
import shopping.international.infrastructure.gateway.common.IExchangeRateApi;
import shopping.international.infrastructure.gateway.common.dto.OpenErLatestRespond;
import shopping.international.types.enums.FxRateProvider;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * open.er-api.com 汇率拉取端口实现
 */
@Component
@RequiredArgsConstructor
public class OpenErExchangeRatePort implements IExchangeRatePort {

    /**
     * open.er-api.com 最新汇率 API URL 模板
     */
    private static final String LATEST_URL_TEMPLATE = "https://open.er-api.com/v6/latest/%s";
    /**
     * 外汇汇率 API 客户端
     */
    private final IExchangeRateApi exchangeRateApi;
    /**
     * 以 UTC 作为采样时间
     */
    private final Clock clock = Clock.systemUTC();

    /**
     * 拉取指定基准币种的最新汇率列表
     *
     * <p>返回的 {@link FxRateLatest#asOf()} 建议表示本次报价拉取/采样时间。</p>
     *
     * @param baseCode 基准币种 (如 USD)
     * @return 最新汇率列表 (可能为空)
     */
    @Override
    public @NotNull List<FxRateLatest> fetchLatest(@NotNull String baseCode) {
        String base = normalizeCurrency(baseCode);
        requireNotNull(base, "baseCode 不能为空");

        String url = String.format(LATEST_URL_TEMPLATE, base);

        try {
            Response<OpenErLatestRespond> resp = exchangeRateApi.latest(url).execute();
            if (!resp.isSuccessful() || resp.body() == null) {
                try (ResponseBody errorBody = resp.errorBody()) {
                    throw new IllegalParamException("拉取汇率失败, HTTP code: " + resp.code() + "错误信息: " + errorBody);
                }
            }

            OpenErLatestRespond body = resp.body();
            if (!"success".equalsIgnoreCase(Objects.requireNonNullElse(body.getResult(), "").strip()))
                throw new IllegalParamException("拉取汇率失败, result=" + body.getResult() + ", errorType=" + body.getErrorType());

            Map<String, BigDecimal> rates = body.getRates();
            if (rates == null || rates.isEmpty())
                return List.of();

            LocalDateTime asOf = LocalDateTime.now(clock);
            List<FxRateLatest> list = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : rates.entrySet()) {
                String quote = normalizeCurrency(Objects.requireNonNullElse(e.getKey(), "").strip());
                BigDecimal rate = e.getValue();
                if (quote.equalsIgnoreCase(base))
                    continue;
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0)
                    continue;
                list.add(FxRateLatest.of(base, quote, rate, asOf, FxRateProvider.OPEN_ER_API));
            }
            return list;
        } catch (IllegalParamException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalParamException("拉取汇率异常: " + e.getMessage());
        }
    }
}

