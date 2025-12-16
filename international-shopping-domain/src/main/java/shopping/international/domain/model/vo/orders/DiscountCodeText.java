package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 折扣码文本值对象
 */
@Getter
@ToString
@EqualsAndHashCode
public final class DiscountCodeText {
    /**
     * 折扣码格式正则
     */
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9]{6}$");
    /**
     * 折扣码文本内容值
     */
    private final String value;

    /**
     * 构造一个折扣码文本值对象
     *
     * @param value 折扣码的文本内容, 必须符合指定格式要求
     */
    private DiscountCodeText(String value) {
        this.value = value;
    }

    /**
     * 从给定的原始字符串创建一个 {@code DiscountCodeText} 对象, 如果原始字符串为空或仅包含空白字符, 则返回 <code>null</code>
     *
     * <p>此方法首先尝试去除输入字符串两端的空白字符, 并将其转换为大写形式. 如果处理后的字符串为空, 或者不符合折扣码格式要求,
     * 将返回 <code>null</code>. 否则, 创建并返回一个新的 {@code DiscountCodeText} 实例.</p>
     *
     * @param raw 原始折扣码文本, 可以为 <code>null</code>
     * @return 如果 <code>raw</code> 为空或不符合格式要求, 返回 <code>null</code>; 否则, 返回一个新的 {@link DiscountCodeText} 实例
     */
    public static DiscountCodeText ofNullable(String raw) {
        if (raw == null)
            return null;
        String trimmed = raw.strip().toUpperCase();
        if (trimmed.isEmpty())
            return null;
        requireNotNull(trimmed, "折扣码不能为空");
        require(PATTERN.matcher(trimmed).matches(), "折扣码格式不合法");
        return new DiscountCodeText(trimmed);
    }
}

