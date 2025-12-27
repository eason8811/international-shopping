package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 金额值对象 (amount + currency)
 *
 * <p>用于订单域的金额计算与不变式校验</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class Money implements Comparable<Money> {
    /**
     * 金额精度
     */
    private static final int SCALE = 2;
    /**
     * 结算货币
     */
    private final String currency;
    /**
     * 金额值
     */
    private final BigDecimal amount;

    /**
     * 构造一个新的 {@code Money} 对象, 代表指定货币和金额的金额值对象
     *
     * @param currency 货币代码, 不能为空
     * @param amount 金额数值, 必须是正数或零, 精度为 {@code SCALE}
     */
    private Money(String currency, BigDecimal amount) {
        this.currency = currency;
        this.amount = amount;
    }

    /**
     * 创建一个金额为零的 {@code Money} 对象
     *
     * @param currency 货币代码, 不能为空
     * @return 一个新的 {@link Money} 对象, 其金额值为零且货币类型由参数指定
     */
    public static Money zero(String currency) {
        return of(currency, BigDecimal.ZERO);
    }

    /**
     * 创建一个新的 {@code Money} 对象, 代表指定货币和金额的金额值对象
     *
     * <p>该方法会检查传入的货币代码是否合法, 并将金额数值标准化到预定义的精度。如果输入参数不满足要求, 则抛出异常</p>
     *
     * @param currency 货币代码, 必须为有效的 3 位字母代码, 不能为空
     * @param amount 金额数值, 必须是正数或零, 将被四舍五入到预设的小数位数
     * @return 新创建的 {@link Money} 实例, 包含已规范化后的货币代码和金额数值
     * @throws IllegalParamException 如果货币代码无效, 或者金额为空, 或者金额为负数
     */
    public static Money of(String currency, BigDecimal amount) {
        String normalizedCurrency = normalizeCurrency(currency);
        requireNotNull(normalizedCurrency, "currency 不能为空");
        requireNotNull(amount, "金额不能为空");
        BigDecimal normalizedAmount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        require(normalizedAmount.compareTo(BigDecimal.ZERO) >= 0, "金额不能为负数");
        return new Money(normalizedCurrency, normalizedAmount);
    }

    /**
     * 将当前金额与另一个 <code>Money</code> 对象表示的金额相加, 并返回一个新的 <code>Money</code> 实例
     *
     * <p>该方法会确保两个金额对象使用相同的货币类型, 如果不一致则抛出异常</p>
     *
     * @param other 要加到当前金额上的另一个 <code>Money</code> 对象, 不能为空
     * @return 一个新的 <code>Money</code> 实例, 其金额值为当前金额与参数金额之和, 货币类型保持不变
     */
    public Money add(Money other) {
        ensureSameCurrency(other);
        return new Money(currency, amount.add(other.amount));
    }

    /**
     * 从当前金额中减去另一个 <code>Money</code> 对象表示的金额, 并返回一个新的 <code>Money</code> 实例
     *
     * <p>该方法会确保两个金额对象使用相同的货币类型, 如果不一致则抛出异常, 此外, 结果金额不能为负数, 否则也会抛出异常</p>
     *
     * @param other 要从当前金额中减去的另一个 <code>Money</code> 对象, 不能为空
     * @return 一个新的 <code>Money</code> 实例, 其金额值为当前金额与参数金额之差, 货币类型保持不变
     * @throws IllegalParamException 如果结果金额小于零
     */
    public Money subtract(Money other) {
        ensureSameCurrency(other);
        BigDecimal result = amount.subtract(other.amount);
        require(result.compareTo(BigDecimal.ZERO) >= 0, "金额不能为负数");
        return new Money(currency, result);
    }

    /**
     * 将当前金额乘以给定的整数乘数, 并返回一个新的 {@code Money} 实例
     *
     * <p>该方法会确保乘数为非负数, 如果乘数小于零则抛出异常。结果金额将被四舍五入到预设的小数位数。</p>
     *
     * @param multiplier 乘数, 必须是非负整数
     * @return 一个新的 {@link Money} 实例, 其金额值为当前金额与乘数相乘后的结果, 货币类型保持不变
     * @throws IllegalParamException 如果乘数小于零
     */
    public Money multiply(int multiplier) {
        require(multiplier >= 0, "乘数不能为负数");
        return new Money(currency, amount.multiply(BigDecimal.valueOf(multiplier)).setScale(SCALE, RoundingMode.HALF_UP));
    }

    /**
     * 判断当前金额是否为零
     *
     * @return 如果当前金额值等于 <code>BigDecimal.ZERO</code>, 返回 <code>true</code>; 否则返回 <code>false</code>
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 确保当前 <code>Money</code> 对象与另一个 <code>Money</code> 对象使用相同的货币类型。
     * 如果两个对象的货币类型不一致, 则抛出异常。
     *
     * @param other 要比较的另一个 <code>Money</code> 对象, 不能为空
     * @throws IllegalArgumentException 如果传入的 <code>Money</code> 对象为空, 或者其货币类型与当前金额对象不匹配
     */
    public void ensureSameCurrency(Money other) {
        requireNotNull(other, "金额不能为空");
        if (!Objects.equals(this.currency, other.currency))
            throw new IllegalParamException("币种不一致: " + this.currency + " vs " + other.currency);
    }

    /**
     * 比较当前 <code>Money</code> 对象与另一个 <code>Money</code> 对象的金额大小。
     *
     * <p>该方法首先检查传入的对象是否为 <code>null</code>, 如果是, 则返回 1 表示当前对象大于 <code>null</code>。接着, 通过调用 {@link #ensureSameCurrency(Money)} 方法来确保两个比较对象使用相同的货币类型, 然后比较两者金额的大小。</p>
     *
     * @param o 要与当前对象进行比较的另一个 <code>Money</code> 对象, 可以为 <code>null</code>
     * @return 如果当前金额小于参数金额, 返回负数; 如果当前金额等于参数金额, 返回 0; 如果当前金额大于参数金额, 返回正数
     * @throws IllegalArgumentException 如果传入的 <code>Money</code> 对象不为空且其货币类型与当前对象不匹配
     */
    @Override
    public int compareTo(@Nullable Money o) {
        if (o == null)
            return 1;
        ensureSameCurrency(o);
        return this.amount.compareTo(o.amount);
    }
}

