package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 手机号值对象 (存储为 E.164 格式)
 * <p>此处仅进行基础格式校验, 国际化规范化可在应用层完成</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class PhoneNumber {
    /**
     * E.164 格式手机号正则表达式
     */
    private static final Pattern SIMPLE = Pattern.compile("^\\+?[0-9\\- ]{6,32}$");

    /**
     * 手机号值
     */
    private final String value;

    /**
     * 构造方法, 私有化, 不允许外部直接创建实例
     *
     * @param value 手机号值 该值应为已验证并格式化的 E.164 格式手机号
     */
    private PhoneNumber(String value) {
        this.value = value;
    }

    /**
     * 工厂方法, 创建 {@link PhoneNumber} 实例
     *
     * @param raw 原始手机号
     * @return {@link PhoneNumber} 实例
     * @throws IllegalParamException 如果提供的手机号为空或格式不正确时抛出
     */
    public static PhoneNumber of(String raw) {
        requireNotNull(raw, "手机号不能为空");
        String val = raw.trim();
        require(!SIMPLE.matcher(val).matches(), "手机号格式不正确");
        return new PhoneNumber(val);
    }

    /**
     * 创建一个可为空的 {@link PhoneNumber} 实例
     *
     * <p>此方法允许传入空字符串或 null 作为参数, 不会进行任何验证直接创建实例
     * 如果需要验证手机号格式, 使用 {@link PhoneNumber#of(String)} 方法</p>
     *
     * @param raw 原始手机号 可以是空字符串或 null
     * @return 返回 {@link PhoneNumber} 实例
     */
    public static PhoneNumber nullableOf(String raw) {
        return new PhoneNumber(raw);
    }
}
