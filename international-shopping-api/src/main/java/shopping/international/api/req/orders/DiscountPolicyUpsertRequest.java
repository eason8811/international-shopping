package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.domain.model.aggregate.orders.DiscountPolicy;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 折扣策略创建/更新请求体 (DiscountPolicyUpsertRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicyUpsertRequest implements Verifiable {
    /**
     * 策略名称 (最大长度 120)
     */
    @Nullable
    private String name;
    /**
     * 应用范围
     */
    @Nullable
    private DiscountApplyScope applyScope;
    /**
     * 折扣策略类型
     */
    @Nullable
    private DiscountStrategyType strategyType;
    /**
     * 百分比折扣 (strategyType=PERCENT 时必填)
     */
    @Nullable
    private String percentOff;
    /**
     * 币种金额配置列表 (整策略携带多币种配置)
     */
    @Nullable
    private List<DiscountPolicyAmountUpsertRequest> amounts;

    /**
     * 通用字段校验与规范化
     *
     * <p>该方法不会强制字段必填, 用于兼容 PATCH 更新场景</p>
     */
    @Override
    public void validate() {
        if (percentOff != null) {
            try {
                BigDecimal percentOffNumber = new BigDecimal(percentOff);
                require(percentOffNumber.compareTo(BigDecimal.ZERO) > 0 && percentOffNumber.compareTo(BigDecimal.valueOf(100L)) <= 0, "percentOff 需在 (0, 100] 区间内");
            } catch (Exception e) {
                throw new IllegalParamException("percentOff 折扣百分比不合法");
            }
        }

        amounts = normalizeDistinctList(
                amounts,
                DiscountPolicyAmountUpsertRequest::getCurrency,
                "同一折扣策略的金额配置币种不可重复"
        );
    }

    /**
     * 创建场景校验
     *
     * <p>创建时需提供策略基础信息, 并根据 {@code strategyType} 提供对应的折扣参数</p>
     */
    @Override
    public void createValidate() {
        validate();
        name = normalizeNotNullField(name, "name 不能为空", s -> s.length() <= 120, "name 长度不能超过 120 个字符");
        requireNotNull(applyScope, "applyScope 不能为空");
        requireNotNull(strategyType, "strategyType 不能为空");

        if (strategyType == DiscountStrategyType.PERCENT) {
            requireNotNull(percentOff, "折扣策略类型为 PERCENT 时 percentOff 不能为空");
            if (amounts != null) {
                for (DiscountPolicyAmountUpsertRequest a : amounts) {
                    if (a == null)
                        continue;
                    require(a.getAmountOff() == null, "折扣策略类型为 PERCENT 时不允许提供 amountOff");
                }
            }
        } else if (strategyType == DiscountStrategyType.AMOUNT) {
            require(percentOff == null, "折扣策略类型为 AMOUNT 时不允许提供 percentOff");
            require(amounts != null && !amounts.isEmpty(), "折扣策略类型为 AMOUNT 时 amounts 不能为空");
            for (DiscountPolicyAmountUpsertRequest a : amounts) {
                requireNotNull(a, "amounts 不能包含 null");
                a.createValidate();
                requireNotNull(a.getAmountOff(), "折扣策略类型为 AMOUNT 时 amountOff 不能为空");
            }
            require(amounts.stream().anyMatch(a -> a != null && DiscountPolicy.DEFAULT_CURRENCY.equalsIgnoreCase(a.getCurrency())),
                    "折扣策略类型为 AMOUNT 时 amounts 必须包含默认币种 " + DiscountPolicy.DEFAULT_CURRENCY);
        }
    }

    /**
     * 更新场景校验
     *
     * <p>更新接口采用整策略更新语义 (需要携带策略基础信息与金额配置)</p>
     */
    @Override
    public void updateValidate() {
        validate();
        name = normalizeNotNullField(name, "name 不能为空", s -> s.length() <= 120, "name 长度不能超过 120 个字符");

        if (strategyType != null && strategyType == DiscountStrategyType.PERCENT) {
            requireNotNull(percentOff, "折扣策略类型为 PERCENT 时 percentOff 不能为空");
            if (amounts != null) {
                for (DiscountPolicyAmountUpsertRequest a : amounts) {
                    if (a == null)
                        continue;
                    require(a.getAmountOff() == null, "折扣策略类型为 PERCENT 时不允许提供 amountOff");
                }
            }
        } else if (strategyType != null && strategyType == DiscountStrategyType.AMOUNT) {
            require(percentOff == null, "折扣策略类型为 AMOUNT 时不允许提供 percentOff");
            require(amounts != null && !amounts.isEmpty(), "折扣策略类型为 AMOUNT 时 amounts 不能为空");
            for (DiscountPolicyAmountUpsertRequest a : amounts) {
                requireNotNull(a, "amounts 不能包含 null");
                a.updateValidate();
                requireNotNull(a.getAmountOff(), "折扣策略类型为 AMOUNT 时 amountOff 不能为空");
            }
            require(amounts.stream().anyMatch(a -> a != null && DiscountPolicy.DEFAULT_CURRENCY.equalsIgnoreCase(a.getCurrency())),
                    "折扣策略类型为 AMOUNT 时 amounts 必须包含默认币种 " + DiscountPolicy.DEFAULT_CURRENCY);
        }
    }

    /**
     * 折扣策略币种金额配置请求体 (DiscountPolicyAmountUpsertRequest)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountPolicyAmountUpsertRequest implements Verifiable {
        /**
         * 币种
         */
        @Nullable
        private String currency;
        /**
         * 固定减免金额 (可为空, 金额字符串), strategyType=AMOUNT 时必填
         */
        @Nullable
        private String amountOff;
        /**
         * 门槛金额 (可为空, 金额字符串)
         */
        @Nullable
        private String minOrderAmount;
        /**
         * 封顶金额 (可为空, 金额字符串)
         */
        @Nullable
        private String maxDiscountAmount;

        /**
         * 校验并规范化金额配置字段
         */
        @Override
        public void validate() {
            currency = normalizeCurrency(currency);
            amountOff = normalizeNullableField(amountOff, "amountOff 不能为空", s -> s.length() <= 64, "amountOff 长度不能超过 64 个字符");
            minOrderAmount = normalizeNullableField(minOrderAmount, "minOrderAmount 不能为空", s -> s.length() <= 64, "minOrderAmount 长度不能超过 64 个字符");
            maxDiscountAmount = normalizeNullableField(maxDiscountAmount, "maxDiscountAmount 不能为空", s -> s.length() <= 64, "maxDiscountAmount 长度不能超过 64 个字符");

            if (amountOff != null)
                try {
                    BigDecimal v = new BigDecimal(amountOff);
                    require(v.compareTo(BigDecimal.ZERO) > 0, "amountOff 必须大于 0");
                } catch (NumberFormatException e) {
                    throw new IllegalParamException("amountOff 数值格式不合法");
                }
            if (minOrderAmount != null)
                try {
                    BigDecimal v = new BigDecimal(minOrderAmount);
                    require(v.compareTo(BigDecimal.ZERO) >= 0, "minOrderAmount 不能为负数");
                } catch (NumberFormatException e) {
                    throw new IllegalParamException("minOrderAmount 数值格式不合法");
                }
            if (maxDiscountAmount != null)
                try {
                    BigDecimal v = new BigDecimal(maxDiscountAmount);
                    require(v.compareTo(BigDecimal.ZERO) > 0, "maxDiscountAmount 必须大于 0");
                } catch (NumberFormatException e) {
                    throw new IllegalParamException("maxDiscountAmount 数值格式不合法");
                }
        }
    }
}
