package shopping.international.domain.model.vo.orders;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 取消原因值对象
 */
@Getter
@ToString
@EqualsAndHashCode
public final class CancelReason {
    /**
     * 取消订单原因最大长度
     */
    private static final int MAX_LEN = 255;
    /**
     * 取消订单原因值
     */
    private final String value;

    /**
     * 构造一个新的 {@code CancelReason} 对象, 代表取消订单的原因
     *
     * @param value 取消原因的文本描述, 必须非空且长度不超过 {@code MAX_LEN}
     */
    private CancelReason(String value) {
        this.value = value;
    }

    /**
     * 根据给定的原始字符串创建一个新的 {@code CancelReason} 对象
     *
     * @param raw 取消原因的原始文本描述, 必须非空且长度不超过 {@code MAX_LEN}
     * @return 新创建的代表取消订单原因的 {@link CancelReason} 实例
     * @throws IllegalParamException 如果提供的取消原因是 <code>null</code>, 空白, 或者长度超过最大限制
     */
    public static CancelReason of(String raw) {
        if (raw == null)
            throw new IllegalParamException("取消原因不能为空");
        String trimmed = raw.strip();
        if (trimmed.isEmpty())
            throw new IllegalParamException("取消原因不能为空");
        require(trimmed.length() <= MAX_LEN, "取消原因不能超过 " + MAX_LEN + " 个字符");
        return new CancelReason(trimmed);
    }
}

