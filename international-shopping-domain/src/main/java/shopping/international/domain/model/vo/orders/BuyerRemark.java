package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 买家留言值对象
 */
@Getter
@ToString
@EqualsAndHashCode
public final class BuyerRemark {
    /**
     * 买家留言最大长度
     */
    private static final int MAX_LEN = 500;
    /**
     * 买家留言值
     */
    private final String value;

    /**
     * 构造一个新的 {@code BuyerRemark} 对象, 代表买家的留言内容
     *
     * <p>该构造函数为私有, 应通过静态工厂方法 {@link #ofNullable(String)} 创建实例</p>
     *
     * @param value 买家留言的具体内容, 不允许为空字符串或超过最大长度限制
     */
    private BuyerRemark(String value) {
        this.value = value;
    }

    /**
     * 从给定的原始字符串创建一个 {@code BuyerRemark} 对象, 如果输入为空或仅包含空白字符, 则返回 <code>null</code>
     *
     * <p>此方法首先尝试去除输入字符串两端的空白字符, 如果处理后的字符串为空, 或者长度超过允许的最大值, 方法将不会创建新的 {@code BuyerRemark} 实例, 而是直接返回 <code>null</code> 或抛出异常。</p>
     *
     * @param raw 原始的买家留言字符串, 可以为 <code>null</code>, 但不能为空白字符串
     * @return 如果输入有效, 返回一个新的 {@link BuyerRemark} 实例; 否则返回 <code>null</code>
     * @throws IllegalParamException 如果处理后的留言内容为空, 或者其长度超过了 {@value BuyerRemark#MAX_LEN} 个字符
     */
    public static BuyerRemark ofNullable(String raw) {
        if (raw == null)
            return null;
        String trimmed = raw.strip();
        if (trimmed.isEmpty())
            return null;
        requireNotNull(trimmed, "留言不能为空");
        require(trimmed.length() <= MAX_LEN, "买家留言不能超过 " + MAX_LEN + " 个字符");
        return new BuyerRemark(trimmed);
    }
}

