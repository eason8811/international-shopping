package shopping.international.domain.adapter.port.common;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.common.FxRateLatest;

import java.util.List;

/**
 * 外部汇率服务端口
 *
 * <p>职责: 从第三方数据源拉取最新汇率报价 (用于写入 fx_rate_latest + fx_rate)。</p>
 */
public interface IExchangeRatePort {

    /**
     * 拉取指定基准币种的最新汇率列表
     *
     * <p>返回的 {@link FxRateLatest#asOf()} 建议表示本次报价拉取/采样时间。</p>
     *
     * @param baseCode 基准币种 (如 USD)
     * @return 最新汇率列表 (可能为空)
     */
    @NotNull
    List<FxRateLatest> fetchLatest(@NotNull String baseCode);
}

