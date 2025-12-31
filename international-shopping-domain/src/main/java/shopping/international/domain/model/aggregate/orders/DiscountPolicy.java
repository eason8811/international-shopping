package shopping.international.domain.model.aggregate.orders;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 折扣策略聚合根, 对应表 discount_policy
 *
 * <p>职责: 维护策略基础不变式 (作用域/类型/门槛/封顶/币种等)。</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class DiscountPolicy implements Verifiable {
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
     * {@link #strategyType}<code> == </code>{@link DiscountStrategyType#AMOUNT AMOUNT} 时的固定折扣金额
     */
    private Long amountOff;
    /**
     * 结算货币 (可以为空, 为空则使用订单的货币)
     */
    private String currency;
    /**
     * 最小订单金额 (可以为空, 为空则不限制)
     */
    private Long minOrderAmount;
    /**
     * 折扣金额封顶 (可以为空, 为空则不限制)
     */
    private Long maxDiscountAmount;
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
     * @param id                折扣策略的唯一标识符
     * @param name              折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope        折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType      折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff        当 {@code strategyType} 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amountOff         当 {@code strategyType} 为 {@link DiscountStrategyType#AMOUNT} 时, 指定固定金额抵扣, 必须大于 0
     * @param currency          货币代码, 如果提供, 必须是合法的货币代码
     * @param minOrderAmount    订单最小金额限制, 如果设置, 应该是非负数
     * @param maxDiscountAmount 最大折扣金额限制, 如果设置, 应该是非负数
     * @param createdAt         折扣策略创建时间
     * @param updatedAt         折扣策略最后更新时间
     */
    private DiscountPolicy(Long id, String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                           BigDecimal percentOff, Long amountOff, String currency,
                           Long minOrderAmount, Long maxDiscountAmount,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.applyScope = applyScope;
        this.strategyType = strategyType;
        this.percentOff = percentOff;
        this.amountOff = amountOff;
        this.currency = currency;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validate();
    }

    /**
     * 创建一个新的折扣策略实例
     *
     * @param name              折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope        折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType      折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff        当 {@code strategyType} 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amountOff         当 {@code strategyType} 为 {@link DiscountStrategyType#AMOUNT} 时, 指定固定金额抵扣, 必须大于 0
     * @param currency          货币代码, 如果提供, 必须是合法的货币代码
     * @param minOrderAmount    订单最小金额限制, 如果设置, 应该是非负数
     * @param maxDiscountAmount 最大折扣金额限制, 如果设置, 应该是非负数
     * @return 新创建的折扣策略实例
     */
    public static DiscountPolicy create(String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                                        @Nullable BigDecimal percentOff, @Nullable Long amountOff,
                                        @Nullable String currency,
                                        @Nullable Long minOrderAmount, @Nullable Long maxDiscountAmount) {
        return new DiscountPolicy(null, name, applyScope, strategyType, percentOff, amountOff, currency,
                minOrderAmount, maxDiscountAmount, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 从给定参数中重新构建一个 <code>DiscountPolicy</code> 实例
     *
     * @param id                折扣策略的唯一标识符
     * @param name              折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope        折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType      折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff        当 <code>strategyType</code> 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amountOff         当 <code>strategyType</code> 为 {@link DiscountStrategyType#AMOUNT} 时, 指定固定金额抵扣, 必须大于 0
     * @param currency          货币代码, 如果提供, 必须是合法的货币代码
     * @param minOrderAmount    订单最小金额限制, 如果设置, 应该是非负数
     * @param maxDiscountAmount 最大折扣金额限制, 如果设置, 应该是非负数
     * @param createdAt         折扣策略创建时间
     * @param updatedAt         折扣策略最后更新时间
     * @return 由给定参数构成的新 <code>DiscountPolicy</code> 实例
     */
    public static DiscountPolicy reconstitute(Long id, String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                                              BigDecimal percentOff, Long amountOff, String currency,
                                              Long minOrderAmount, Long maxDiscountAmount,
                                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DiscountPolicy(id, name, applyScope, strategyType, percentOff, amountOff, currency,
                minOrderAmount, maxDiscountAmount, createdAt, updatedAt);
    }

    /**
     * 更新折扣策略的信息。此方法允许修改折扣策略的名称、应用范围、策略类型以及相关的数值参数。
     *
     * @param name 折扣策略的名称, 不能为空且长度不得超过 120 个字符
     * @param applyScope 折扣应用范围, 可以是 {@link DiscountApplyScope#ORDER} 或 {@link DiscountApplyScope#ITEM}, 不能为空
     * @param strategyType 折扣策略类型, 可以是 {@link DiscountStrategyType#PERCENT} 或 {@link DiscountStrategyType#AMOUNT}, 不能为空
     * @param percentOff 当 {@code strategyType} 为 {@link DiscountStrategyType#PERCENT} 时, 指定百分比折扣, 值应在 0 到 100 之间
     * @param amountOff 当 {@code strategyType} 为 {@link DiscountStrategyType#AMOUNT} 时, 指定固定金额抵扣, 必须大于 0
     * @param currency 货币代码, 如果提供, 必须是合法的货币代码
     * @param minOrderAmount 订单最小金额限制, 如果设置, 应该是非负数
     * @param maxDiscountAmount 最大折扣金额限制, 如果设置, 应该是非负数
     */
    public void update(String name, DiscountApplyScope applyScope, DiscountStrategyType strategyType,
                       @Nullable BigDecimal percentOff, @Nullable Long amountOff,
                       @Nullable String currency,
                       @Nullable Long minOrderAmount, @Nullable Long maxDiscountAmount) {
        if (name != null)
            this.name = name.strip();
        if (applyScope != null)
            this.applyScope = applyScope;
        if (strategyType != null)
            this.strategyType = strategyType;
        if (percentOff != null)
            this.percentOff = percentOff;
        if (amountOff != null)
            this.amountOff = amountOff;
        if (currency != null)
            this.currency = currency.strip();
        if (minOrderAmount != null)
            this.minOrderAmount = minOrderAmount;
        if (maxDiscountAmount != null)
            this.maxDiscountAmount = maxDiscountAmount;
        validate();
    }

    /**
     * 验证折扣策略的有效性, 包括名称、作用域、折扣类型以及根据折扣类型设置的百分比或金额等参数。
     * <p>此方法确保以下条件被满足:
     * <ul>
     *     <li>策略名称既不为空也不仅由空白字符组成, 且长度不超过 120 个字符</li>
     *     <li>作用域和折扣类型都不为 null</li>
     *     <li>如果折扣类型是百分比, 则百分比值在 0 到 100 之间(包含边界)</li>
     *     <li>如果折扣类型是固定金额, 则该金额必须大于 0</li>
     *     <li>货币代码, 如果提供, 必须是合法的货币代码</li>
     *     <li>订单最小金额限制和最大折扣金额限制, 如果设置了, 必须是非负数</li>
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
        }
        if (strategyType == DiscountStrategyType.AMOUNT) {
            requireNotNull(amountOff, "amountOff 不能为空");
            require(amountOff > 0, "amountOff 必须大于 0");
        }
        if (currency != null) {
            String normalized = normalizeCurrency(currency);
            requireNotNull(normalized, "currency 不合法");
            this.currency = normalized;
        }
        if (minOrderAmount != null)
            require(minOrderAmount >= 0, "minOrderAmount 不能为负数");
        if (maxDiscountAmount != null)
            require(maxDiscountAmount >= 0, "maxDiscountAmount 不能为负数");
    }
}
