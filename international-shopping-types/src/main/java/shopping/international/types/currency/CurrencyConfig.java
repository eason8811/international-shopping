package shopping.international.types.currency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 货币配置 (来自 currency 表)
 *
 * <p>用于「最小货币单位 long」与「展示金额 BigDecimal」之间的统一换算与舍入规则承载</p>
 *
 * @param code            货币代码
 * @param minorUnit       货币最小单位
 * @param roundingMode    舍入模式
 * @param cashRoundingInc 货币金额舍入增量 (仅用于现金)
 */
public record CurrencyConfig(@NotNull String code,
                             int minorUnit,
                             @NotNull RoundingMode roundingMode,
                             @Nullable BigDecimal cashRoundingInc) {

    /**
     * 构造函数, 用于初始化货币配置对象
     *
     * @param code            货币代码, 必须为非空且格式正确的 ISO 4217 3位字母代码
     * @param minorUnit       货币最小单位, 需在 0~9 之间
     * @param roundingMode    舍入模式, 不得为空
     * @param cashRoundingInc 货币金额舍入增量 (仅用于现金), 可以为 null, 若不为 null 则必须大于 0
     * @throws IllegalParamException 如果任一参数不符合要求
     */
    public CurrencyConfig {
        code = normalizeCurrency(code);
        requireNotNull(code, "currency 不能为空");
        require(minorUnit >= 0 && minorUnit <= 9, "minor_unit 需在 0~9 之间");
        requireNotNull(roundingMode, "rounding_mode 不能为空");
        if (cashRoundingInc != null)
            require(cashRoundingInc.compareTo(BigDecimal.ZERO) > 0, "cash_rounding_inc 必须大于 0");
    }

    /**
     * 返回给定货币代码的默认配置
     *
     * @param code 货币代码, 必须为非空且格式正确的 ISO 4217 3位字母代码
     * @return 返回一个带有默认设置的 {@link CurrencyConfig} 对象, 包含了传入的货币代码, 默认的小数位数 (2), 四舍五入模式 ({@link RoundingMode#HALF_UP}) 和 null 的现金舍入增量
     */
    public static @NotNull CurrencyConfig defaultFor(@NotNull String code) {
        return new CurrencyConfig(code, 2, RoundingMode.HALF_UP, null);
    }

    /**
     * 将最小货币单位转换为展示金额
     *
     * <p>此方法基于当前配置的 {@code minorUnit} 值, 将给定的最小货币单位数量转换为相应的展示金额, 适用于需要将内部存储的最小单位金额转换为用户可读格式的场景</p>
     *
     * @param minorAmount 最小货币单位的数量, 需要转换成展示金额
     * @return 返回转换后的展示金额, 类型为 {@link BigDecimal}, 该值反映了以当前货币配置下正确的展示形式
     */
    public @NotNull BigDecimal toMajor(long minorAmount) {
        return BigDecimal.valueOf(minorAmount, minorUnit);
    }

    /**
     * 将最小货币单位转换为展示金额, 支持传入 <code>null</code> 值
     *
     * <p>此方法基于当前配置的 {@code minorUnit} 值, 将给定的最小货币单位数量转换为相应的展示金额, 如果输入为 <code>null</code>, 则返回 <code>null</code></p>
     *
     * @param minorAmount 最小货币单位的数量, 需要转换成展示金额, 可以为 <code>null</code>
     * @return 返回转换后的展示金额, 类型为 {@link BigDecimal}, 如果输入为 <code>null</code>, 则返回 <code>null</code>; 否则, 该值反映了以当前货币配置下正确的展示形式
     */
    public @Nullable BigDecimal toMajorNullable(@Nullable Long minorAmount) {
        return minorAmount == null ? null : toMajor(minorAmount);
    }

    /**
     * 将给定的主要金额转换为最小货币单位的精确数值表示
     *
     * <p>此方法基于当前配置的 {@code minorUnit} 值, 将传入的主要金额 ({@link BigDecimal}) 转换为其对应的最小货币单位数量, 并确保结果是一个整数, 如果输入金额的小数位数超过 {@code minorUnit} 所允许的范围, 或者金额为负数, 则会抛出异常</p>
     *
     * @param majorAmount 主要金额, 必须为非空且非负数
     * @return 返回转换后的最小货币单位数量, 类型为 {@code long}, 代表了主要金额在当前货币配置下的最小单位数量
     * @throws IllegalParamException 如果输入金额为负数, 或小数位数超过当前配置的限制
     */
    public long toMinorExact(@NotNull BigDecimal majorAmount) {
        requireNotNull(majorAmount, "金额不能为空");
        require(majorAmount.compareTo(BigDecimal.ZERO) >= 0, "金额不能为负数");
        try {
            BigDecimal scaled = majorAmount.setScale(minorUnit, RoundingMode.UNNECESSARY);
            BigDecimal minor = scaled.movePointRight(minorUnit);
            return minor.longValueExact();
        } catch (ArithmeticException e) {
            throw IllegalParamException.of("金额小数位超过该币种 minor_unit=" + minorUnit + " 的限制");
        }
    }

    /**
     * 将给定的主要金额转换为最小货币单位的精确数值表示, 支持传入 <code>null</code> 值
     *
     * <p>此方法基于当前配置的 {@code minorUnit} 值, 将传入的主要金额 ({@link BigDecimal}) 转换为其对应的最小货币单位数量, 如果输入为 <code>null</code>, 则返回 <code>null</code></p>
     *
     * @param majorAmount 主要金额, 可以为 <code>null</code>
     * @return 返回转换后的最小货币单位数量, 类型为 {@code Long}, 如果输入为 <code>null</code>, 则返回 <code>null</code>; 否则, 该值反映了主要金额在当前货币配置下的最小单位数量
     */
    public @Nullable Long toMinorExactNullable(@Nullable BigDecimal majorAmount) {
        return majorAmount == null ? null : toMinorExact(majorAmount);
    }
}
