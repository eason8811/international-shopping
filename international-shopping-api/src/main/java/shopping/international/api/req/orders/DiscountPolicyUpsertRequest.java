package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.math.BigDecimal;

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
     * 固定金额折扣 (strategyType=AMOUNT 时必填, 金额字符串)
     */
    @Nullable
    private String amountOff;
    /**
     * 币种 (strategyType=AMOUNT 时建议必填)
     */
    @Nullable
    private String currency;
    /**
     * 最小订单金额限制 (可选, 金额字符串)
     */
    @Nullable
    private String minOrderAmount;
    /**
     * 最大折扣金额上限 (可选, 金额字符串)
     */
    @Nullable
    private String maxDiscountAmount;

    /**
     * 通用字段校验与规范化
     *
     * <p>该方法不会强制字段必填, 用于兼容 PATCH 更新场景</p>
     */
    @Override
    public void validate() {
        currency = normalizeCurrency(currency);
        if (percentOff != null) {
            try {
                BigDecimal percentOffNumber = new BigDecimal(percentOff);
                require(percentOffNumber.compareTo(BigDecimal.ZERO) > 0 && percentOffNumber.compareTo(BigDecimal.valueOf(100L)) <= 0, "percentOff 需在 (0, 100] 区间内");
            } catch (Exception e) {
                throw new IllegalParamException("percentOff 折扣百分比不合法");
            }
        }
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
            require(amountOff == null, "折扣策略类型为 PERCENT 时不允许提供 amountOff");
        } else if (strategyType == DiscountStrategyType.AMOUNT) {
            amountOff = normalizeNotNullField(amountOff, "折扣策略类型为 AMOUNT 时 amountOff 不能为空",
                    s -> s.length() <= 64, "amountOff 长度不能超过 64 个字符");
            require(percentOff == null, "折扣策略类型为 AMOUNT 时不允许提供 percentOff");
        }
    }

    /**
     * 更新场景校验
     *
     * <p>更新时为 null 的字段表示不更新; 若更新折扣参数, 则需满足对应策略类型的约束</p>
     */
    @Override
    public void updateValidate() {
        validate();
        name = normalizeNullableField(name, "name 不能为空", s -> s.length() <= 120, "name 长度不能超过 120 个字符");
        amountOff = normalizeNullableField(amountOff, "amountOff 不能为空", s -> s.length() <= 64, "amountOff 长度不能超过 64 个字符");

        require(name != null || applyScope != null || strategyType != null || percentOff != null || amountOff != null || currency != null
                        || minOrderAmount != null || maxDiscountAmount != null,
                "更新折扣策略时至少需要提供一个要更新的字段");

        if (strategyType == null) {
            require(!(percentOff != null && amountOff != null), "percentOff 与 amountOff 不能同时提供");
            return;
        }

        if (strategyType == DiscountStrategyType.PERCENT) {
            requireNotNull(percentOff, "strategyType=PERCENT 时 percentOff 不能为空");
            require(amountOff == null, "strategyType=PERCENT 时不允许提供 amountOff");
        } else if (strategyType == DiscountStrategyType.AMOUNT) {
            requireNotNull(amountOff, "strategyType=AMOUNT 时 amountOff 不能为空");
            require(percentOff == null, "strategyType=AMOUNT 时不允许提供 percentOff");
        }
    }
}

