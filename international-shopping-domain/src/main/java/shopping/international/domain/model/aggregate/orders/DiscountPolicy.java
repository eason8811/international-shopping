package shopping.international.domain.model.aggregate.orders;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.orders.DiscountPolicyAmount;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 折扣策略聚合根, 对应表 discount_policy
 *
 * <p>职责: 维护策略基础不变式 (作用域/类型/折扣参数/门槛/封顶等), 其中金额相关字段按币种下沉到 {@code discount_policy_amount}</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class DiscountPolicy implements Verifiable {
    /**
     * 全站默认币种 (用于缺失币种配置时回退)
     */
    public static final String DEFAULT_CURRENCY = "USD";

    /**
     * 折扣策略主键 ID
     */
    private final Long id;
    /**
     * 策略名称
     */
    private String name;
    /**
     * 策略作用域
     */
    private DiscountApplyScope applyScope;
    /**
     * 策略类型
     */
    private DiscountStrategyType strategyType;

    /**
     * {@link #strategyType}<code> == </code>{@link DiscountStrategyType#PERCENT PERCENT} 时的折扣百分比
     */
    private BigDecimal percentOff;
    /**
     * 币种金额配置列表 (对应表 discount_policy_amount)
     */
    @NotNull
    private List<DiscountPolicyAmount> amounts;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private final LocalDateTime updatedAt;

    /**
     * 构造一个新的折扣策略实例
     *
     * @param id           折扣策略的唯一标识符
     * @param name         折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope   折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType 折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff   当 {@code strategyType} 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amounts      按币种金额配置列表
     * @param createdAt    折扣策略创建时间
     * @param updatedAt    折扣策略最后更新时间
     */
    private DiscountPolicy(Long id, String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                           BigDecimal percentOff, List<DiscountPolicyAmount> amounts,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.applyScope = applyScope;
        this.strategyType = strategyType;
        this.percentOff = percentOff;
        this.amounts = amounts == null ? List.of() : List.copyOf(amounts);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validate();
    }

    /**
     * 创建一个新的折扣策略实例
     *
     * @param name         折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope   折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType 折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff   当 {@code strategyType} 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amounts      按币种金额配置列表
     * @return 新创建的折扣策略实例
     */
    public static DiscountPolicy create(String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                                        @Nullable BigDecimal percentOff,
                                        @Nullable List<DiscountPolicyAmount> amounts) {
        return new DiscountPolicy(null, name, applyScope, strategyType, percentOff, amounts, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 从给定参数中重新构建一个 <code>DiscountPolicy</code> 实例
     *
     * @param id           折扣策略的唯一标识符
     * @param name         折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope   折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType 折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff   当 <code>strategyType</code> 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amounts      按币种金额配置列表
     * @param createdAt    折扣策略创建时间
     * @param updatedAt    折扣策略最后更新时间
     * @return 由给定参数构成的新 <code>DiscountPolicy</code> 实例
     */
    public static DiscountPolicy reconstitute(Long id, String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                                              BigDecimal percentOff, List<DiscountPolicyAmount> amounts,
                                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DiscountPolicy(id, name, applyScope, strategyType, percentOff, amounts, createdAt, updatedAt);
    }

    /**
     * 更新折扣策略的信息。此方法允许修改折扣策略的名称、应用范围、策略类型以及相关的数值参数。
     *
     * @param name         折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope   折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType 折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff   当 {@code strategyType} 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amounts      按币种金额配置列表, 传入 null 表示不更新
     */
    public void update(String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                       @Nullable BigDecimal percentOff,
                       @Nullable List<DiscountPolicyAmount> amounts) {
        if (name != null)
            this.name = name.strip();
        if (applyScope != null)
            this.applyScope = applyScope;
        if (strategyType != null)
            this.strategyType = strategyType;
        if (percentOff != null)
            this.percentOff = percentOff;
        if (amounts != null)
            this.amounts = List.copyOf(amounts);
        validate();
    }

    /**
     * 解析给定币种对应的金额配置 (缺失时回退到全站默认币种 USD)
     *
     * @param currency 币种, 允许为空 (为空视为 USD)
     * @return 命中的币种金额配置, 若策略未配置任何金额信息则返回 null
     */
    public @Nullable DiscountPolicyAmount resolveAmount(@Nullable String currency) {
        if (amounts.isEmpty())
            return null;
        String normalized = normalizeCurrency(currency);
        for (DiscountPolicyAmount a : amounts)
            if (a != null && normalized.equals(a.getCurrency()))
                return a;
        if (DEFAULT_CURRENCY.equals(normalized))
            return null;
        for (DiscountPolicyAmount a : amounts)
            if (a != null && DEFAULT_CURRENCY.equals(a.getCurrency()))
                return a;
        return null;
    }

    /**
     * 验证折扣策略的有效性, 包括名称、作用域、折扣类型以及根据折扣类型设置的百分比或金额等参数。
     * <p>此方法确保以下条件被满足:
     * <ul>
     *     <li>策略名称既不为空也不仅由空白字符组成, 且长度不超过 120 个字符</li>
     *     <li>作用域和折扣类型都不为 null</li>
     *     <li>如果折扣类型是百分比, 则百分比值在 0 到 100 之间(包含边界)</li>
     *     <li>如果折扣类型是固定金额, 则需要提供币种金额配置且包含默认币种 USD 的 amountOff</li>
     *     <li>门槛/封顶/固定折扣均按币种配置, 且金额不得为负数</li>
     * </ul>
     * <p>如果任何验证失败, 将抛出 {@link IllegalParamException} 异常
     *
     * @throws IllegalParamException 当任一验证条件未被满足时抛出
     */
    @Override
    public void validate() {
        requireNotBlank(name, "策略名称不能为空");
        require(name.strip().length() <= 120, "策略名称最长 120 个字符");
        requireNotNull(applyScope, "作用域不能为空");
        requireNotNull(strategyType, "折扣类型不能为空");

        if (strategyType == DiscountStrategyType.PERCENT) {
            requireNotNull(percentOff, "percentOff 不能为空");
            require(percentOff.compareTo(BigDecimal.ZERO) >= 0 && percentOff.compareTo(BigDecimal.valueOf(100)) <= 0, "percentOff 需在 0-100 之间");
            for (DiscountPolicyAmount a : amounts) {
                if (a == null)
                    continue;
                a.validate();
                require(a.getAmountOffMinor() == null, "strategyType=PERCENT 时不允许配置 amountOff");
            }
        }
        if (strategyType == DiscountStrategyType.AMOUNT) {
            require(percentOff == null, "strategyType=AMOUNT 时不允许配置 percentOff");
            require(!amounts.isEmpty(), "strategyType=AMOUNT 时 amounts 不能为空");
            Set<String> seen = new HashSet<>();
            boolean hasDefault = false;
            for (DiscountPolicyAmount a : amounts) {
                requireNotNull(a, "amounts 不能包含 null");
                a.validate();
                require(seen.add(a.getCurrency()), "amounts 中存在重复的 currency: " + a.getCurrency());
                requireNotNull(a.getAmountOffMinor(), "strategyType=AMOUNT 时 amountOff 不能为空");
                if (DEFAULT_CURRENCY.equals(a.getCurrency()))
                    hasDefault = true;
            }
            require(hasDefault, "strategyType=AMOUNT 时必须配置默认币种 USD 的金额项");
        }

        if (!amounts.isEmpty()) {
            Set<String> seen = new HashSet<>();
            for (DiscountPolicyAmount a : amounts) {
                if (a == null)
                    continue;
                a.validate();
                require(seen.add(a.getCurrency()), "amounts 中存在重复的 currency: " + a.getCurrency());
            }
        }
    }
}
