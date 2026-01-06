package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.common.ICurrencyRepository;
import shopping.international.domain.adapter.repository.orders.IDiscountRepository;
import shopping.international.domain.model.aggregate.orders.DiscountCode;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.domain.model.entity.orders.DiscountPolicyAmount;
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
import java.util.ArrayList;
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
        if (policy.getStrategyType() == DiscountStrategyType.AMOUNT) {
            List<DiscountPolicyAmount> full = deriveMissingFxAutoAmounts(policy.getAmounts(), DiscountPolicy.DEFAULT_CURRENCY);
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

        List<DiscountPolicyAmount> fullAmounts = toUpdate.getAmounts();
        if (toUpdate.getStrategyType() == DiscountStrategyType.AMOUNT)
            fullAmounts = deriveMissingFxAutoAmounts(toUpdate.getAmounts(), DiscountPolicy.DEFAULT_CURRENCY);

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
     *     <li>未传入的启用币种: 使用 base(USD) + latest 汇率自动换算并补齐</li>
     * </ul>
     *
     * @param requested    请求的折扣策略金额列表, 包含已知的一些货币的折扣金额信息
     * @param baseCurrency 基础货币代码, 用于基于此货币计算其他货币的折扣金额
     * @return 补全了所有缺失货币折扣金额后的完整列表
     * @throws IllegalParamException 当提供的 amounts 列表中不包含基础货币的金额项时抛出
     */
    private @NotNull List<DiscountPolicyAmount> deriveMissingFxAutoAmounts(@NotNull List<DiscountPolicyAmount> requested,
                                                                           @NotNull String baseCurrency) {
        Map<String, DiscountPolicyAmount> amountByCurrencyMap = requested.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(a -> a.getCurrency().toUpperCase(), Function.identity(), (a, b) -> b));

        DiscountPolicyAmount baseAmount = amountByCurrencyMap.get(baseCurrency);
        if (baseAmount == null)
            throw new IllegalParamException("amounts 必须包含全站默认币种 " + baseCurrency + " 的金额项");

        Set<String> quoteCodes = currencyRepository.listEnabledCodes().stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .map(String::toUpperCase)
                .filter(c -> !c.isBlank())
                .distinct()
                .filter(c -> !baseCurrency.equalsIgnoreCase(c))
                .filter(c -> !amountByCurrencyMap.containsKey(c))
                .collect(Collectors.toSet());

        if (quoteCodes.isEmpty())
            return requested;

        Map<String, FxRateLatest> latestMap = fxRateService.getLatestByQuotes(baseCurrency, quoteCodes);

        CurrencyConfig baseCfg = currencyConfigService.get(baseCurrency);
        Clock clock = Clock.systemUTC();

        List<DiscountPolicyAmount> derived = new ArrayList<>();
        for (String quote : quoteCodes) {
            FxRateLatest latest = latestMap.get(quote);
            requireNotNull(latest, "汇率 '" + baseCurrency + "' -> '" + quote + "' 不存在, 无法自动计算折扣金额配置");
            require(latest.isFresh(clock, FX_MAX_AGE), "汇率 '" + baseCurrency + "' -> '" + quote + "' 已过期, 无法自动计算折扣金额配置");

            CurrencyConfig quoteCfg = currencyConfigService.get(quote);
            Long amountOff = baseAmount.getAmountOffMinor() == null
                    ? null
                    : convertMinor(baseCfg, quoteCfg, baseAmount.getAmountOffMinor(), latest.rate());
            Long minOrder = baseAmount.getMinOrderAmountMinor() == null
                    ? null
                    : convertMinor(baseCfg, quoteCfg, baseAmount.getMinOrderAmountMinor(), latest.rate());
            Long maxDiscount = baseAmount.getMaxDiscountAmountMinor() == null
                    ? null
                    : convertMinor(baseCfg, quoteCfg, baseAmount.getMaxDiscountAmountMinor(), latest.rate());

            derived.add(DiscountPolicyAmount.of(quote, amountOff, minOrder, maxDiscount));
        }

        List<DiscountPolicyAmount> full = new ArrayList<>(requested);
        full.addAll(derived);
        return full;
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
