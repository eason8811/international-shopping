package shopping.international.domain.service.common.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.common.IExchangeRatePort;
import shopping.international.domain.adapter.repository.common.ICurrencyRepository;
import shopping.international.domain.adapter.repository.common.IFxRateRepository;
import shopping.international.domain.model.vo.common.FxRateLatest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 汇率领域服务实现
 */
@Service
@RequiredArgsConstructor
public class FxRateService implements shopping.international.domain.service.common.IFxRateService {

    /**
     * 汇率数据仓储服务
     */
    private final IFxRateRepository fxRateRepository;
    /**
     * 货币数据仓储服务
     */
    private final ICurrencyRepository currencyRepository;
    /**
     * 汇率数据源服务
     */
    private final IExchangeRatePort exchangeRatePort;

    /**
     * 查询最新汇率 (可能为空)
     *
     * @param baseCode  基准币种, 如 USD
     * @param quoteCode 报价币种, 如 EUR
     * @return 最新汇率快照值对象, 如果没有找到符合条件的汇率则返回 null
     */
    @Override
    public @Nullable FxRateLatest getLatest(@NotNull String baseCode, @NotNull String quoteCode) {
        return fxRateRepository.findLatest(baseCode, quoteCode);
    }

    /**
     * 批量查询最新汇率信息
     *
     * <p>此方法根据给定的基准币种和报价币种集合, 返回一个包含最新汇率快照值对象的映射, 键为报价币种, 值为对应的最新汇率快照</p>
     *
     * @param baseCode   基准币种, 如 USD
     * @param quoteCodes 报价币种集合, 如 Set.of("EUR", "GBP")
     * @return 映射, 键为报价币种, 值为 {@link FxRateLatest} 对象
     */
    @Override
    public @NotNull Map<String, FxRateLatest> getLatestByQuotes(@NotNull String baseCode, @NotNull Set<String> quoteCodes) {
        return fxRateRepository.findLatestByQuotes(baseCode, quoteCodes);
    }

    /**
     * 同步最新汇率: 拉取外部服务并落库 (latest + history)
     *
     * <p>同步币种集合由 currency.enabled 推导</p>
     *
     * @param baseCode 基准币种 (默认 USD)
     */
    @Override
    public void syncLatest(@NotNull String baseCode) {
        String base = normalizeCurrency(baseCode);
        requireNotNull(base, "baseCode 不能为空");

        List<String> enabled = currencyRepository.listEnabledCodes();
        if (enabled.isEmpty())
            return;

        Set<String> quoteCodes = enabled.stream()
                .map(c -> normalizeCurrency(Objects.requireNonNullElse(c, "").strip()))
                .filter(c -> !c.equalsIgnoreCase(base))
                .collect(Collectors.toSet());
        if (quoteCodes.isEmpty())
            return;

        List<FxRateLatest> fetched = exchangeRatePort.fetchLatest(base);
        if (fetched.isEmpty())
            return;

        List<FxRateLatest> toPersist = fetched.stream()
                .filter(Objects::nonNull)
                .filter(r -> base.equalsIgnoreCase(r.baseCode()))
                .filter(r -> quoteCodes.contains(r.quoteCode()))
                .toList();

        fxRateRepository.upsertLatestAndInsertHistory(toPersist);
    }
}

