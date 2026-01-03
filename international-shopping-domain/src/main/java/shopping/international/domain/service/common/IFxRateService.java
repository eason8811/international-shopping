package shopping.international.domain.service.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.common.FxRateLatest;

import java.util.Map;
import java.util.Set;

/**
 * 汇率领域服务
 */
public interface IFxRateService {

    /**
     * 查询最新汇率 (可能为空)
     *
     * @param baseCode  基准币种, 如 USD
     * @param quoteCode 报价币种, 如 EUR
     * @return 最新汇率快照值对象, 如果没有找到符合条件的汇率则返回 null
     */
    @Nullable
    FxRateLatest getLatest(@NotNull String baseCode, @NotNull String quoteCode);

    /**
     * 批量查询最新汇率信息
     *
     * <p>此方法根据给定的基准币种和报价币种集合, 返回一个包含最新汇率快照值对象的映射, 键为报价币种, 值为对应的最新汇率快照</p>
     *
     * @param baseCode   基准币种, 如 USD
     * @param quoteCodes 报价币种集合, 如 Set.of("EUR", "GBP")
     * @return 映射, 键为报价币种, 值为 {@link FxRateLatest} 对象
     */
    @NotNull
    Map<String, FxRateLatest> getLatestByQuotes(@NotNull String baseCode, @NotNull Set<String> quoteCodes);

    /**
     * 同步最新汇率: 拉取外部服务并落库 (latest + history)
     *
     * <p>同步币种集合由 currency.enabled 推导</p>
     *
     * @param baseCode 基准币种 (默认 USD)
     */
    void syncLatest(@NotNull String baseCode);
}

