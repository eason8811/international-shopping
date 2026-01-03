package shopping.international.infrastructure.adapter.repository.common;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.common.IFxRateRepository;
import shopping.international.domain.model.vo.common.FxRateLatest;
import shopping.international.infrastructure.dao.common.FxRateLatestMapper;
import shopping.international.infrastructure.dao.common.FxRateMapper;
import shopping.international.infrastructure.dao.common.po.FxRateLatestPO;
import shopping.international.infrastructure.dao.common.po.FxRatePO;
import shopping.international.types.enums.FxRateProvider;

import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * FX 汇率仓储实现 (fx_rate_latest / fx_rate)
 */
@Repository
@RequiredArgsConstructor
public class FxRateRepository implements IFxRateRepository {

    /**
     * 最新外汇汇率快照 Mapper
     */
    private final FxRateLatestMapper fxRateLatestMapper;
    /**
     * 历史外汇汇率 Mapper
     */
    private final FxRateMapper fxRateMapper;

    /**
     * 查询指定币种对的最新汇率
     *
     * @param baseCode  基准币种 (如 USD)
     * @param quoteCode 报价币种 (如 EUR)
     * @return 最新汇率快照 (可能为空)
     */
    @Override
    public @Nullable FxRateLatest findLatest(@NotNull String baseCode, @NotNull String quoteCode) {
        String base = normalizeCurrency(baseCode);
        String quote = normalizeCurrency(quoteCode);
        requireNotNull(base, "baseCode 不能为空");
        requireNotNull(quote, "quoteCode 不能为空");
        FxRateLatestPO po = fxRateLatestMapper.selectOneLatest(base, quote);
        if (po == null)
            return null;
        FxRateProvider provider = FxRateProvider.of(po.getProvider());
        return FxRateLatest.of(po.getBaseCode(), po.getQuoteCode(), po.getRate(), po.getAsOf(), provider);
    }

    /**
     * 批量查询指定报价币种集合的最新汇率
     *
     * @param baseCode   基准币种
     * @param quoteCodes 报价币种集合
     * @return quoteCode → latest 映射
     */
    @Override
    public @NotNull Map<String, FxRateLatest> findLatestByQuotes(@NotNull String baseCode, @NotNull Set<String> quoteCodes) {
        String base = normalizeCurrency(baseCode);
        requireNotNull(base, "baseCode 不能为空");
        if (quoteCodes.isEmpty())
            return Collections.emptyMap();

        List<String> quotes = quoteCodes.stream()
                .map(c -> normalizeCurrency(Objects.requireNonNullElse(c, "").strip()))
                .distinct()
                .toList();
        if (quotes.isEmpty())
            return Collections.emptyMap();

        List<FxRateLatestPO> pos = fxRateLatestMapper.selectLatestByQuotes(base, quotes);
        if (pos == null || pos.isEmpty())
            return Collections.emptyMap();

        Map<String, FxRateLatest> map = new HashMap<>();
        for (FxRateLatestPO po : pos) {
            if (po == null || po.getQuoteCode() == null)
                continue;
            FxRateProvider provider = FxRateProvider.of(po.getProvider());
            FxRateLatest vo = FxRateLatest.of(po.getBaseCode(), po.getQuoteCode(), po.getRate(), po.getAsOf(), provider);
            map.put(vo.quoteCode(), vo);
        }
        return map;
    }

    /**
     * upsert 最新快照 + 写入历史表 (必须同时写入历史)
     *
     * @param rates 最新汇率列表
     */
    @Override
    @Transactional
    public void upsertLatestAndInsertHistory(@NotNull List<FxRateLatest> rates) {
        requireNotNull(rates, "rates 不能为空");
        if (rates.isEmpty())
            return;

        List<FxRateLatestPO> latest = rates.stream()
                .filter(Objects::nonNull)
                .map(r -> FxRateLatestPO.builder()
                        .baseCode(r.baseCode())
                        .quoteCode(r.quoteCode())
                        .rate(r.rate())
                        .asOf(r.asOf())
                        .provider(r.provider().code())
                        .build())
                .toList();
        fxRateLatestMapper.upsertBatch(latest);

        List<FxRatePO> history = rates.stream()
                .filter(Objects::nonNull)
                .map(r -> FxRatePO.builder()
                        .baseCode(r.baseCode())
                        .quoteCode(r.quoteCode())
                        .rate(r.rate())
                        .asOf(r.asOf())
                        .provider(r.provider().code())
                        .build())
                .toList();
        fxRateMapper.insertIgnoreBatch(history);
    }
}

