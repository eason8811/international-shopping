package shopping.international.domain.service.common.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.common.ICurrencyRepository;
import shopping.international.domain.model.vo.common.CurrencyProfile;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.types.currency.CurrencyConfig;

import java.math.RoundingMode;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 货币配置查询服务 (带缓存)
 */
@Service
@RequiredArgsConstructor
public class CurrencyConfigService implements ICurrencyConfigService {
    /**
     * 缓存过期时间
     */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    /**
     * 货币仓储服务
     */
    private final ICurrencyRepository currencyRepository;
    /**
     * 缓存
     */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 获取指定币种配置 (内部可缓存)
     *
     * @param currency ISO 4217 代码 (如 USD)
     * @return 配置 (若不存在则返回默认配置)
     */
    @Override
    public @NotNull CurrencyConfig get(@NotNull String currency) {
        String code = normalizeCurrency(currency);
        requireNotNull(code, "currency 不能为空");

        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(code);
        if (cached != null && now - cached.loadedAtMs < CACHE_TTL_MS)
            return cached.config;

        CurrencyConfig loaded = loadFromRepoOrDefault(code);
        cache.put(code, new CacheEntry(loaded, now));
        return loaded;
    }

    /**
     * 根据给定的货币代码从仓库加载货币配置, 如果找不到或配置未启用, 则返回默认配置
     *
     * @param code 货币代码 (ISO 4217)
     * @return 对应的 <code>CurrencyConfig</code> 实例, 如果仓库中没有找到有效的配置, 则返回一个基于给定代码的默认配置
     */
    private CurrencyConfig loadFromRepoOrDefault(String code) {
        CurrencyProfile profile = currencyRepository.findByCode(code);
        if (profile == null || Boolean.FALSE.equals(profile.enabled()))
            return CurrencyConfig.defaultFor(code);

        int minorUnit = profile.minorUnit() == null ? 2 : profile.minorUnit();
        RoundingMode roundingMode = parseRoundingMode(profile.roundingMode());
        return new CurrencyConfig(code, minorUnit, roundingMode, profile.cashRoundingInc());
    }

    /**
     * 解析数据库中的四舍五入模式字符串, 并返回对应的 <code>RoundingMode</code> 枚举值
     *
     * @param dbValue 数据库中存储的四舍五入模式字符串, 如果为空或空白, 则默认返回 <code>RoundingMode.HALF_UP</code>
     *                特殊地, 如果输入为 "BANKERS", 则返回 <code>RoundingMode.HALF_EVEN</code>
     * @return 对应的 <code>RoundingMode</code> 枚举值
     */
    private static RoundingMode parseRoundingMode(String dbValue) {
        if (dbValue == null || dbValue.isBlank())
            return RoundingMode.HALF_UP;
        String normalized = dbValue.strip().toUpperCase();
        if (Objects.equals(normalized, "BANKERS"))
            return RoundingMode.HALF_EVEN;
        return RoundingMode.valueOf(normalized);
    }

    /**
     * <p>代表缓存中的一个条目, 包含了货币配置信息以及该条目被加载的时间戳, 用于实现基于时间的缓存过期机制</p>
     *
     * @param config     货币配置信息, 不能为空
     * @param loadedAtMs 该缓存条目被加载时的时间戳 (毫秒), 用于判断缓存是否过期
     */
    private record CacheEntry(@NotNull CurrencyConfig config, long loadedAtMs) {
    }
}

