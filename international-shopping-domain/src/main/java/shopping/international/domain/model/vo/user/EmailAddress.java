package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.Locale;
import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 邮箱值对象 (统一小写)
 */
@Getter
@EqualsAndHashCode
@ToString
public final class EmailAddress {
    /**
     * 邮箱正则表达式
     */
    private static final Pattern PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 邮箱值
     */
    private final String value;

    /**
     * 构造方法, 私有化, 不允许外部直接创建实例
     *
     * @param value 邮箱值 该值应为已验证并格式化的邮箱地址
     */
    private EmailAddress(String value) {
        this.value = value;
    }

    /**
     * 工厂方法, 创建 {@link EmailAddress} 实例
     *
     * @param raw 原始邮箱地址
     * @return {@link EmailAddress} 实例
     * @throws IllegalParamException 如果提供的邮箱为空或格式不正确时抛出
     */
    public static EmailAddress of(String raw) {
        requireNotNull(raw, "邮箱不能为空");
        String val = raw.trim().toLowerCase(Locale.ROOT);
        require(!PATTERN.matcher(val).matches(), "邮箱格式不正确");
        return new EmailAddress(val);
    }
}
