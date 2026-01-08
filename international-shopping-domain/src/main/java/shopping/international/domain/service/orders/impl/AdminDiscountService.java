package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.common.ICurrencyRepository;
import shopping.international.domain.adapter.repository.orders.IDiscountRepository;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.entity.orders.DiscountPolicyAmount;
import shopping.international.domain.model.enums.orders.DiscountPolicyAmountSource;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.DiscountCodeSearchCriteria;
import shopping.international.domain.model.vo.orders.DiscountPolicySearchCriteria;
import shopping.international.domain.model.vo.orders.OrderDiscountAppliedSearchCriteria;
import shopping.international.domain.model.vo.common.FxRateLatest;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.common.IFxRateService;
import shopping.international.domain.service.orders.IAdminDiscountService;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧折扣管理领域服务默认实现
 */
@Service
@RequiredArgsConstructor
public class AdminDiscountService implements IAdminDiscountService {

    /**
     * 折扣仓储
     */
    private final IDiscountRepository discountRepository;
    /**
     * 货币仓储 (用于读取启用币种集合)
     */
    private final ICurrencyRepository currencyRepository;
    /**
     * 汇率服务 (用于自动派生缺失币种金额配置)
     */
    private final IFxRateService fxRateService;
    /**
     * 货币配置服务 (用于金额换算与舍入)
     */
    private final ICurrencyConfigService currencyConfigService;
    /**
     * 最大容忍汇率更新时间
     */
    private static final Duration FX_MAX_AGE = Duration.ofHours(8);

    /**
     * 查询折扣策略列表
     *
     * @param pageQuery 分页条件
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<DiscountPolicy> listPolicies(@NotNull PageQuery pageQuery, @NotNull DiscountPolicySearchCriteria criteria) {
        pageQuery.validate();
        criteria.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<DiscountPolicy> items = discountRepository.pagePolicies(criteria, offset, limit);
        long total = discountRepository.countPolicies(criteria);
        return new PageResult<>(items, total);
    }

    /**
     * 创建折扣策略
     *
     * @param policy 新策略
     * @return 保存后的策略
     */
    @Override
    public @NotNull DiscountPolicy createPolicy(@NotNull DiscountPolicy policy) {
        discountRepository.findPolicyByName(policy.getName())
                .ifPresent(p -> {
                    throw new ConflictException("折扣策略已存在");
                });
        if (policy.getStrategyType() == DiscountStrategyType.AMOUNT) {
            List<DiscountPolicyAmount> full = deriveFxAmountsForUpsert(policy.getAmounts(), List.of(), DiscountPolicy.DEFAULT_CURRENCY);
            policy.update(null, null, null, null, full);
        }
        return discountRepository.savePolicy(policy);
    }

    /**
     * 更新折扣策略
     *
     * @param policyId 策略 ID
     * @param toUpdate 用于更新的 Policy 对象
     * @return 更新后的策略
     */
    @Override
    public @NotNull DiscountPolicy updatePolicy(@NotNull Long policyId, @NotNull DiscountPolicy toUpdate) {
        DiscountPolicy policy = discountRepository.findPolicyById(policyId)
                .orElseThrow(() -> new IllegalParamException("折扣策略不存在"));
        if (toUpdate.getName() != null)
            discountRepository.findPolicyByName(toUpdate.getName())
                    .ifPresent(p -> {
                        throw new ConflictException("折扣策略已存在");
                    });
        List<DiscountPolicyAmount> fullAmounts = toUpdate.getAmounts();
        if (toUpdate.getStrategyType() == DiscountStrategyType.AMOUNT)
            fullAmounts = deriveFxAmountsForUpsert(toUpdate.getAmounts(), policy.getAmounts(), DiscountPolicy.DEFAULT_CURRENCY);

        policy.update(
                toUpdate.getName(),
                toUpdate.getApplyScope(),
                toUpdate.getStrategyType(),
                toUpdate.getPercentOff(),
                fullAmounts
        );
        return discountRepository.updatePolicy(policy);
    }

    /**
     * 重算指定折扣策略(AMOUNT)的 FX_AUTO / 缺失币种金额配置
     *
     * <p>不会覆盖已有 MANUAL 币种；基于 USD 基准金额 + 最新汇率派生其余币种</p>
     *
     * @param policyId 策略 ID
     * @return 受影响的币种列表
     */
    @Override
    public @NotNull List<String> recomputeFxAmounts(@NotNull Long policyId) {
        DiscountPolicy policy = discountRepository.findPolicyById(policyId)
                .orElseThrow(() -> new IllegalParamException("折扣策略不存在"));
        List<DiscountPolicyAmount> full = recomputeFxAmountsForPolicy(policy);
        policy.update(null, null, null, null, full);
        discountRepository.updatePolicy(policy);
        return full.stream().filter(Objects::nonNull).map(DiscountPolicyAmount::getCurrency).distinct().toList();
    }

    /**
     * 全量重算所有折扣策略(AMOUNT)的 FX_AUTO / 缺失币种金额配置
     *
     * @param batchSize 每批处理的策略数量
     * @return 处理的策略数量
     */
    @Override
    public int recomputeFxAmountsAll(int batchSize) {
        int size = Math.max(1, Math.min(batchSize, 500));
        int offset = 0;
        int processed = 0;
        DiscountPolicySearchCriteria criteria = DiscountPolicySearchCriteria.builder()
                .strategyType(DiscountStrategyType.AMOUNT)
                .build();
        while (true) {
            List<DiscountPolicy> policies = discountRepository.pagePolicies(criteria, offset, size);
            if (policies.isEmpty())
                break;
            for (DiscountPolicy p : policies) {
                if (p == null || p.getId() == null)
                    continue;
                try {
                    List<DiscountPolicyAmount> full = recomputeFxAmountsForPolicy(p);
                    p.update(null, null, null, null, full);
                    discountRepository.updatePolicy(p);
                    processed++;
                } catch (Exception ignore) {
                    // 跳过缺失 USD 金额项等不满足派生条件的策略
                }
            }
            offset += policies.size();
            if (policies.size() < size)
                break;
        }
        return processed;
    }

    /**
     * 将指定折扣策略(AMOUNT)的金额配置模式切换为 MANUAL (冻结金额, 清空 FX 元数据)
     *
     * @param policyId 策略 ID
     * @return 受影响的币种列表
     */
    @Override
    public @NotNull List<String> switchPolicyAmountsToManual(@NotNull Long policyId) {
        DiscountPolicy policy = discountRepository.findPolicyById(policyId)
                .orElseThrow(() -> new IllegalParamException("折扣策略不存在"));
        require(policy.getStrategyType() == DiscountStrategyType.AMOUNT, "仅 AMOUNT 策略支持金额配置模式切换");

        List<DiscountPolicyAmount> full = recomputeFxAmountsForPolicy(policy);
        List<DiscountPolicyAmount> manual = full.stream()
                .filter(Objects::nonNull)
                .map(a -> DiscountPolicyAmount.of(a.getCurrency(), a.getAmountOffMinor(), a.getMinOrderAmountMinor(), a.getMaxDiscountAmountMinor()))
                .toList();
        policy.update(null, null, null, null, manual);
        discountRepository.updatePolicy(policy);
        return manual.stream().map(DiscountPolicyAmount::getCurrency).distinct().toList();
    }

    /**
     * 将指定折扣策略(AMOUNT)的金额配置模式切换为 FX_AUTO (除 USD 外全部按汇率派生)
     *
     * @param policyId 策略 ID
     * @return 受影响的币种列表
     */
    @Override
    public @NotNull List<String> switchPolicyAmountsToFxAuto(@NotNull Long policyId) {
        DiscountPolicy policy = discountRepository.findPolicyById(policyId)
                .orElseThrow(() -> new IllegalParamException("折扣策略不存在"));
        require(policy.getStrategyType() == DiscountStrategyType.AMOUNT, "仅 AMOUNT 策略支持金额配置模式切换");

        DiscountPolicyAmount base = policy.resolveAmount(DiscountPolicy.DEFAULT_CURRENCY);
        requireNotNull(base, "策略缺少全站默认币种 " + DiscountPolicy.DEFAULT_CURRENCY + " 的金额项");
        DiscountPolicyAmount baseManual = DiscountPolicyAmount.of(
                DiscountPolicy.DEFAULT_CURRENCY,
                base.getAmountOffMinor(),
                base.getMinOrderAmountMinor(),
                base.getMaxDiscountAmountMinor()
        );
        List<DiscountPolicyAmount> full = deriveFxAmountsForUpsert(List.of(baseManual), List.of(), DiscountPolicy.DEFAULT_CURRENCY);
        policy.update(null, null, null, null, full);
        discountRepository.updatePolicy(policy);
        return full.stream().filter(Objects::nonNull).map(DiscountPolicyAmount::getCurrency).distinct().toList();
    }

    /**
     * 删除折扣策略
     *
     * @param policyId 策略 ID
     */
    @Override
    public void deletePolicy(@NotNull Long policyId) {
        if (discountRepository.countCodeByPolicyId(policyId) > 0)
            throw new ConflictException("折扣策略正被折扣码使用, 无法删除");
        discountRepository.deletePolicy(policyId);
    }

    /**
     * 查询折扣码列表
     *
     * @param pageQuery 分页查询条件
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<DiscountCode> listCodes(@NotNull PageQuery pageQuery, @NotNull DiscountCodeSearchCriteria criteria) {
        pageQuery.validate();
        criteria.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<DiscountCode> items = discountRepository.pageCodes(criteria, offset, limit);
        long total = discountRepository.countCodes(criteria);
        return new PageResult<>(items, total);
    }

    /**
     * 创建折扣码
     *
     * @param code 新折扣码
     * @return 保存后的折扣码
     */
    @Override
    public @NotNull DiscountCode createCode(@NotNull DiscountCode code) {
        return discountRepository.saveCode(code);
    }

    /**
     * 更新折扣码
     *
     * @param codeId   折扣码 ID
     * @param toUpdate 用于跟新的 Code 对象
     * @return 更新后的折扣码
     */
    @Override
    public @NotNull DiscountCode updateCode(@NotNull Long codeId, @NotNull DiscountCode toUpdate) {
        DiscountCode code = discountRepository.findCodeById(codeId)
                .orElseThrow(() -> new IllegalParamException("折扣码不存在"));
        if (toUpdate.getCode() != null && !toUpdate.getCode().equals(code.getCode()))
            throw new ConflictException("折扣码不支持修改");
        code.update(
                toUpdate.getPolicyId(),
                toUpdate.getName(),
                toUpdate.getScopeMode(),
                toUpdate.getExpiresAt()
        );
        return discountRepository.updateCode(code);
    }

    /**
     * 删除折扣码
     *
     * @param codeId 折扣码 ID
     */
    @Override
    public void deleteCode(@NotNull Long codeId) {
        discountRepository.deleteCode(codeId);
    }

    /**
     * 获取折扣码适用商品映射
     *
     * @param codeId 折扣码 ID
     * @return SPU ID 列表
     */
    @Override
    public @NotNull List<Long> listCodeProducts(@NotNull Long codeId) {
        return discountRepository.listCodeProductIds(codeId);
    }

    /**
     * 覆盖设置折扣码适用商品映射
     *
     * @param codeId     折扣码 ID
     * @param productIds SPU ID 列表
     * @return 生效后的 SPU ID 列表
     */
    @Override
    public @NotNull List<Long> replaceCodeProducts(@NotNull Long codeId, @NotNull List<Long> productIds) {
        return discountRepository.replaceCodeProducts(codeId, productIds);
    }

    /**
     * 查询折扣实际使用流水
     *
     * @param pageQuery 分页查询条件
     * @param criteria  筛选条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<OrderDiscountAppliedView> listOrderDiscountApplied(@NotNull PageQuery pageQuery,
                                                                                  @NotNull OrderDiscountAppliedSearchCriteria criteria) {
        pageQuery.validate();
        criteria.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<OrderDiscountAppliedView> items = discountRepository.pageOrderDiscountApplied(criteria, offset, limit);
        long total = discountRepository.countOrderDiscountApplied(criteria);
        return new PageResult<>(items, total);
    }

    /**
     * 写入时派生折扣金额配置:
     * <ul>
     *     <li>必须包含 base(USD) 币种项</li>
     *     <li>传入的非 base 币种: 视为人工设置, 不自动换算</li>
     *     <li>未传入的启用币种:
     *         <ul>
     *             <li>若历史上该币种为人工设置, 则保留人工金额</li>
     *             <li>否则使用 base(USD) + latest 汇率自动换算并补齐</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param requested    请求的折扣策略金额列表, 包含已知的一些货币的折扣金额信息
     * @param existed      数据库中已有的折扣策略金额列表 (用于识别并保留人工设置项)
     * @param baseCurrency 基础货币代码, 用于基于此货币计算其他货币的折扣金额
     * @return 补全了所有缺失货币折扣金额后的完整列表
     * @throws IllegalParamException 当提供的 amounts 列表中不包含基础货币的金额项时抛出
     */
    private @NotNull List<DiscountPolicyAmount> deriveFxAmountsForUpsert(@NotNull List<DiscountPolicyAmount> requested,
                                                                         @NotNull List<DiscountPolicyAmount> existed,
                                                                         @NotNull String baseCurrency) {
        // 将传入的 amountList 转换为 currency -> amount 的 Map
        LinkedHashMap<String, DiscountPolicyAmount> amountByCurrencyMap = requested.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        amount -> amount.getCurrency().strip().toUpperCase(),
                        Function.identity(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
        String normalizedBaseCurrency = baseCurrency.strip().toUpperCase();
        // 将已存在的 amountList 转换为 currency -> amount 的 Map
        Map<String, DiscountPolicyAmount> existedAmountByCurrencyMap = existed.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        a -> a.getCurrency().toUpperCase(),
                        Function.identity(),
                        (a, b) -> b
                ));

        DiscountPolicyAmount baseAmount = amountByCurrencyMap.get(normalizedBaseCurrency);
        if (baseAmount == null)
            throw new IllegalParamException("amounts 必须包含全站默认币种 " + normalizedBaseCurrency + " 的金额项");

        // 获取所有启用的 currency (包括全站默认币种)
        Set<String> enabledCodes = currencyRepository.listEnabledCodes().stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .map(String::toUpperCase)
                .filter(c -> !c.isBlank())
                .collect(Collectors.toSet());
        enabledCodes.add(normalizedBaseCurrency);

        // 提取不为 '全站默认币种' 且 没有传入的 且 在数据库中模式不为 'MANUAL' 的 currency, 即需要进行汇率换算的币种
        Set<String> autoQuotes = enabledCodes.stream()
                .filter(c -> !normalizedBaseCurrency.equalsIgnoreCase(c))
                .filter(c -> !amountByCurrencyMap.containsKey(c)) // 传入的非 base 币种视为人工设置
                .filter(c -> {
                    DiscountPolicyAmount old = existedAmountByCurrencyMap.get(c);
                    return old == null || old.getSource() != DiscountPolicyAmountSource.MANUAL;
                })
                .collect(Collectors.toSet());

        // 根据需要进行汇率换算的币种, 获取最新汇率
        Map<String, FxRateLatest> latestMap = autoQuotes.isEmpty()
                ? Map.of()
                : fxRateService.getLatestByQuotes(normalizedBaseCurrency, autoQuotes);

        CurrencyConfig baseCfg = currencyConfigService.get(normalizedBaseCurrency);
        Clock clock = Clock.systemUTC();
        LocalDateTime computedAt = LocalDateTime.now(clock);

        List<DiscountPolicyAmount> full = new ArrayList<>();
        full.add(baseAmount);           // 添加全站默认币种的 amount
        full.addAll(                    // 添加传入的不为 null 且 非全站默认币种的 amount
                amountByCurrencyMap.values().stream()
                        .filter(Objects::nonNull)
                        .filter(a -> !normalizedBaseCurrency.equalsIgnoreCase(a.getCurrency()))
                        .toList()
        );
        // 添加可用币种中, 非全站默认币种 且 没有传入的 且 已经存在的 且 为人工模式的 amount, 已经存在但模式为 FX_AUTO 进行汇率换算后添加
        for (String enabled : enabledCodes) {
            if (normalizedBaseCurrency.equalsIgnoreCase(enabled))
                continue;
            if (amountByCurrencyMap.containsKey(enabled))
                continue;
            DiscountPolicyAmount old = existedAmountByCurrencyMap.get(enabled);
            if (old != null && old.getSource() == DiscountPolicyAmountSource.MANUAL) {
                full.add(old);
                continue;
            }

            FxRateLatest latest = latestMap.get(enabled);
            requireNotNull(latest, "汇率 '" + normalizedBaseCurrency + "' -> '" + enabled + "' 不存在, 无法自动计算折扣金额配置");
            require(latest.isFresh(clock, FX_MAX_AGE), "汇率 '" + normalizedBaseCurrency + "' -> '" + enabled + "' 已过期, 无法自动计算折扣金额配置");

            CurrencyConfig quoteCfg = currencyConfigService.get(enabled);
            Long amountOff = baseAmount.getAmountOffMinor() == null
                    ? null
                    : convertMinor(baseCfg, quoteCfg, baseAmount.getAmountOffMinor(), latest.rate());
            Long minOrder = baseAmount.getMinOrderAmountMinor() == null
                    ? null
                    : convertMinor(baseCfg, quoteCfg, baseAmount.getMinOrderAmountMinor(), latest.rate());
            Long maxDiscount = baseAmount.getMaxDiscountAmountMinor() == null
                    ? null
                    : convertMinor(baseCfg, quoteCfg, baseAmount.getMaxDiscountAmountMinor(), latest.rate());

            full.add(DiscountPolicyAmount.fxAuto(
                    enabled,
                    amountOff,
                    minOrder,
                    maxDiscount,
                    normalizedBaseCurrency,
                    latest.rate(),
                    latest.asOf(),
                    latest.provider(),
                    computedAt
            ));
        }

        // 保留“已存在但当前未启用”的人工配置, 避免在仅更新部分字段时被覆盖清理
        for (DiscountPolicyAmount old : existedAmountByCurrencyMap.values()) {
            if (old == null || old.getCurrency() == null)
                continue;
            String c = old.getCurrency().strip().toUpperCase();
            if (c.isBlank() || normalizedBaseCurrency.equalsIgnoreCase(c))
                continue;
            if (amountByCurrencyMap.containsKey(c))
                continue;
            if (enabledCodes.contains(c))
                continue;
            if (old.getSource() == DiscountPolicyAmountSource.MANUAL)
                full.add(old);
        }
        return full;
    }

    /**
     * 重算策略的 FX_AUTO / 缺失币种金额配置 (不覆盖已有 MANUAL)
     *
     * @param policy 需要重新计算其外币金额的折扣策略对象 必须非空且其策略类型为 AMOUNT
     * @return 一个非空的 {@link DiscountPolicyAmount} 列表 包含了重新计算后的所有货币金额项
     * @throws IllegalArgumentException 如果提供的 <code>policy</code> 的策略类型不是 AMOUNT
     */
    private @NotNull List<DiscountPolicyAmount> recomputeFxAmountsForPolicy(@NotNull DiscountPolicy policy) {
        requireNotNull(policy.getStrategyType(), "strategyType 不能为空");
        require(policy.getStrategyType() == DiscountStrategyType.AMOUNT, "仅 AMOUNT 策略支持金额配置重算");

        DiscountPolicyAmount base = policy.resolveAmount(DiscountPolicy.DEFAULT_CURRENCY);
        requireNotNull(base, "策略缺少全站默认币种 " + DiscountPolicy.DEFAULT_CURRENCY + " 的金额项");

        // 重算只依赖 USD 基准金额, 并确保 USD 为 MANUAL (清理可能存在的 FX 元数据)
        DiscountPolicyAmount baseManual = DiscountPolicyAmount.of(
                DiscountPolicy.DEFAULT_CURRENCY,
                base.getAmountOffMinor(),
                base.getMinOrderAmountMinor(),
                base.getMaxDiscountAmountMinor()
        );
        return deriveFxAmountsForUpsert(List.of(baseManual), policy.getAmounts(), DiscountPolicy.DEFAULT_CURRENCY);
    }

    /**
     * 将基础货币的最小单位金额转换为目标货币的最小单位金额, 并根据给定的加成基点进行调整
     * baseMinor(USD) -> quoteMinor
     * <p>markup_bps 在换算前应用，最终舍入由 quoteCfg.roundingMode + minorUnit 决定</p>
     *
     * @param baseCfg   基础货币配置信息
     * @param quoteCfg  目标货币配置信息
     * @param baseMinor 基础货币的最小单位金额
     * @param rate      汇率, 用于从基础货币转换到目标货币
     * @return 转换后并经过加成调整的目标货币的最小单位金额
     */
    private static long convertMinor(@NotNull CurrencyConfig baseCfg,
                                     @NotNull CurrencyConfig quoteCfg,
                                     long baseMinor,
                                     @NotNull BigDecimal rate) {
        require(baseMinor >= 0, "金额不能为负数");
        requireNotNull(rate, "rate 不能为空");
        require(rate.compareTo(BigDecimal.ZERO) > 0, "rate 必须大于 0");
        BigDecimal baseMajor = baseCfg.toMajor(baseMinor);
        BigDecimal quoteMajor = baseMajor.multiply(rate);
        return quoteCfg.toMinorRounded(quoteMajor);
    }
}
