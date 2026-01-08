package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountPolicyAmountSource;
import shopping.international.types.enums.FxRateProvider;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 折扣策略币种金额配置实体 (对应表 discount_policy_amount), 归属 DiscountPolicy 聚合
 *
 * <p>用于按币种定义折扣金额 / 门槛金额 / 封顶金额, 统一采用最小货币单位 long 进行存储与计算</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "currency")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DiscountPolicyAmount implements Verifiable {
    /**
     * 币种 (ISO 4217 3 位字母代码)
     */
    private String currency;
    /**
     * 固定折扣金额 (最小货币单位), 当策略类型为 AMOUNT 时使用, 可为空
     */
    private Long amountOffMinor;
    /**
     * 门槛金额 (最小货币单位), 可为空
     */
    private Long minOrderAmountMinor;
    /**
     * 封顶金额 (最小货币单位), 可为空
     */
    private Long maxDiscountAmountMinor;

    /**
     * 金额来源 (手动/汇率自动派生)
     */
    private DiscountPolicyAmountSource source;
    /**
     * FX 派生基准币种 (通常 USD), source=FX_AUTO 时有效
     */
    @Nullable
    private String derivedFrom;
    /**
     * FX 派生汇率 (1 derived_from = fx_rate currency)
     */
    @Nullable
    private BigDecimal fxRate;
    /**
     * FX 汇率时间点/采样时间
     */
    @Nullable
    private LocalDateTime fxAsOf;
    /**
     * FX 数据源
     */
    @Nullable
    private FxRateProvider fxProvider;
    /**
     * 派生计算时间
     */
    @Nullable
    private LocalDateTime computedAt;

    /**
     * 构造币种金额配置
     *
     * @param currency               币种
     * @param amountOffMinor         固定折扣金额 (最小货币单位), 可为空
     * @param minOrderAmountMinor    门槛金额 (最小货币单位), 可为空
     * @param maxDiscountAmountMinor 封顶金额 (最小货币单位), 可为空
     * @return 新配置实体
     */
    public static DiscountPolicyAmount of(@Nullable String currency,
                                          @Nullable Long amountOffMinor,
                                          @Nullable Long minOrderAmountMinor,
                                          @Nullable Long maxDiscountAmountMinor) {
        DiscountPolicyAmount amount = new DiscountPolicyAmount(
                currency,
                amountOffMinor,
                minOrderAmountMinor,
                maxDiscountAmountMinor,
                DiscountPolicyAmountSource.MANUAL,
                null,
                null,
                null,
                null,
                null
        );
        amount.validate();
        return amount;
    }

    /**
     * 创建一个通过自动汇率派生得到的折扣金额配置
     *
     * @param currency               币种, 必填
     * @param amountOffMinor         固定折扣金额 (最小货币单位), 可空
     * @param minOrderAmountMinor    门槛金额 (最小货币单位), 可空
     * @param maxDiscountAmountMinor 封顶金额 (最小货币单位), 可空
     * @param derivedFrom            派生基准币种, 必填
     * @param fxRate                 汇率, 必须大于 0
     * @param fxAsOf                 汇率采样时间, 必填
     * @param fxProvider             数据源, 必填
     * @param computedAt             计算时间, 必填
     * @return 新配置实体
     */
    public static @NotNull DiscountPolicyAmount fxAuto(@NotNull String currency,
                                                       @Nullable Long amountOffMinor,
                                                       @Nullable Long minOrderAmountMinor,
                                                       @Nullable Long maxDiscountAmountMinor,
                                                       @NotNull String derivedFrom,
                                                       @NotNull BigDecimal fxRate,
                                                       @NotNull LocalDateTime fxAsOf,
                                                       @NotNull FxRateProvider fxProvider,
                                                       @NotNull LocalDateTime computedAt) {
        DiscountPolicyAmount amount = new DiscountPolicyAmount(
                currency,
                amountOffMinor,
                minOrderAmountMinor,
                maxDiscountAmountMinor,
                DiscountPolicyAmountSource.FX_AUTO,
                derivedFrom,
                fxRate,
                fxAsOf,
                fxProvider,
                computedAt
        );
        amount.validate();
        return amount;
    }

    /**
     * 从持久化数据重新构建实体
     */
    public static @NotNull DiscountPolicyAmount reconstitute(@NotNull String currency,
                                                             @Nullable Long amountOffMinor,
                                                             @Nullable Long minOrderAmountMinor,
                                                             @Nullable Long maxDiscountAmountMinor,
                                                             @NotNull DiscountPolicyAmountSource source,
                                                             @Nullable String derivedFrom,
                                                             @Nullable BigDecimal fxRate,
                                                             @Nullable LocalDateTime fxAsOf,
                                                             @Nullable FxRateProvider fxProvider,
                                                             @Nullable LocalDateTime computedAt) {
        DiscountPolicyAmount amount = new DiscountPolicyAmount(
                currency,
                amountOffMinor,
                minOrderAmountMinor,
                maxDiscountAmountMinor,
                source,
                derivedFrom,
                fxRate,
                fxAsOf,
                fxProvider,
                computedAt
        );
        amount.validate();
        return amount;
    }

    /**
     * 校验币种金额配置字段合法性
     */
    @Override
    public void validate() {
        currency = normalizeCurrency(currency);
        requireNotNull(currency, "currency 不能为空");
        if (source == null)
            source = DiscountPolicyAmountSource.MANUAL;

        if (amountOffMinor != null)
            require(amountOffMinor > 0, "amountOff 需大于 0");
        if (minOrderAmountMinor != null)
            require(minOrderAmountMinor >= 0, "minOrderAmount 不能为负数");
        if (maxDiscountAmountMinor != null)
            require(maxDiscountAmountMinor >= 0, "maxDiscountAmount 不能为负数");

        if (source == DiscountPolicyAmountSource.FX_AUTO) {
            derivedFrom = normalizeCurrency(derivedFrom);
            requireNotNull(derivedFrom, "derivedFrom 不能为空");
            require(!currency.equalsIgnoreCase(derivedFrom), "currency 不能与 derivedFrom 相同");
            requireNotNull(fxRate, "fxRate 不能为空");
            require(fxRate.compareTo(BigDecimal.ZERO) > 0, "fxRate 必须大于 0");
            requireNotNull(fxAsOf, "fxAsOf 不能为空");
            requireNotNull(fxProvider, "fxProvider 不能为空");
            requireNotNull(computedAt, "computedAt 不能为空");
        }
    }
}
