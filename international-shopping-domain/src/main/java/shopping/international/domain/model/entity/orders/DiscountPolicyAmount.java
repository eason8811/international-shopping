package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

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
        DiscountPolicyAmount amount = new DiscountPolicyAmount(currency, amountOffMinor, minOrderAmountMinor, maxDiscountAmountMinor);
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

        if (amountOffMinor != null)
            require(amountOffMinor > 0, "amountOff 需大于 0");
        if (minOrderAmountMinor != null)
            require(minOrderAmountMinor >= 0, "minOrderAmount 不能为负数");
        if (maxDiscountAmountMinor != null)
            require(maxDiscountAmountMinor >= 0, "maxDiscountAmount 不能为负数");
    }
}

