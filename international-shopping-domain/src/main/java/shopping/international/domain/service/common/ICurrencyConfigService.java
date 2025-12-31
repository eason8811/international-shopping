package shopping.international.domain.service.common;

import org.jetbrains.annotations.NotNull;
import shopping.international.types.currency.CurrencyConfig;

/**
 * 货币配置查询服务
 *
 * <p>提供 currency 表的配置读取与缓存, 用于金额换算与舍入规则的统一来源</p>
 */
public interface ICurrencyConfigService {

    /**
     * 获取指定币种配置 (内部可缓存)
     *
     * @param currency ISO 4217 代码 (如 USD)
     * @return 配置 (若不存在则返回默认配置)
     */
    @NotNull
    CurrencyConfig get(@NotNull String currency);
}

