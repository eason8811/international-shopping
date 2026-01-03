package shopping.international.domain.adapter.repository.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.common.FxRateLatest;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 汇率仓储接口 (fx_rate_latest / fx_rate)
 *
 * <p>latest 用于派生与换算；history 用于审计/回放。</p>
 */
public interface IFxRateRepository {

    /**
     * 查询指定币种对的最新汇率
     *
     * @param baseCode  基准币种 (如 USD)
     * @param quoteCode 报价币种 (如 EUR)
     * @return 最新汇率快照 (可能为空)
     */
    @Nullable
    FxRateLatest findLatest(@NotNull String baseCode, @NotNull String quoteCode);

    /**
     * 批量查询指定报价币种集合的最新汇率
     *
     * @param baseCode    基准币种
     * @param quoteCodes  报价币种集合
     * @return quoteCode → latest 映射
     */
    @NotNull
    Map<String, FxRateLatest> findLatestByQuotes(@NotNull String baseCode, @NotNull Set<String> quoteCodes);

    /**
     * upsert 最新快照 + 写入历史表 (必须同时写入历史)
     *
     * @param rates 最新汇率列表
     */
    void upsertLatestAndInsertHistory(@NotNull List<FxRateLatest> rates);
}
