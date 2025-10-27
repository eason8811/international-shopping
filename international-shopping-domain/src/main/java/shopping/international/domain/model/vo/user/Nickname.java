package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * 昵称值对象
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Nickname {
    /**
     * 昵称值
     */
    private final String value;

    /**
     * 构造方法, 私有化, 不允许外部直接创建实例
     *
     * @param value 昵称值 需要通过工厂方法 {@link Nickname#of(String)} 创建实例
     */
    private Nickname(String value) {
        this.value = value;
    }

    /**
     * 工厂方法, 创建 {@link Nickname} 实例
     *
     * @param raw 原始昵称
     * @return {@link Nickname} 实例
     */
    public static Nickname of(String raw) {
        if (raw == null)
            throw new IllegalParamException("昵称不能为空");
        String val = raw.trim();
        if (val.isEmpty() || val.length() > 64)
            throw new IllegalParamException("昵称长度必须为 1~64 个字符");
        return new Nickname(val);
    }
}
