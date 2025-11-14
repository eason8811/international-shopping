package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 手机号值对象 (存储为 E.164 风格: 仅 + 和数字, 无分隔符)
 * <p>此处仅进行基础格式校验, 国际化规范化可在应用层完成</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class PhoneNumber {

    /**
     * 规范化后的手机号正则:
     * 可选 '+' + 6~32 位数字
     */
    private static final Pattern SIMPLE = Pattern.compile("^\\+?[0-9]{6,32}$");

    /**
     * 手机号值 (规范化后)
     */
    private final String value;

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
        String normalized = normalize(raw);
        require(SIMPLE.matcher(normalized).matches(), "手机号格式不正确");
        return new PhoneNumber(normalized);
    }

    /**
     * 工厂方法, 创建可接受空值的 {@link PhoneNumber} 实例
     *
     * @param raw 原始手机号 可以为 null 或空字符串, 如果非空则需要符合规范化规则
     * @return {@link PhoneNumber} 实例
     * @throws IllegalParamException 如果提供的手机号不为空但格式不正确时抛出
     */
    public static PhoneNumber nullableOf(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new PhoneNumber(null);
        }
        return of(raw);
    }

    /**
     * 将手机号规范化为 E.164 格式, 即仅包含 '+' 和数字, 不含任何分隔符
     *
     * @param raw 原始手机号字符串
     * @return 规范化后的手机号字符串
     * @throws IllegalParamException 如果提供的手机号为空或格式不正确时抛出
     */
    private static String normalize(String raw) {
        requireNotNull(raw, "手机号不能为空");
        String trimmed = raw.trim();
        require(!trimmed.isEmpty(), "手机号不能为空");

        StringBuilder sb = new StringBuilder(trimmed.length());
        boolean firstChar = true;

        for (int i = 0; i < trimmed.length(); i++) {
            char chr = trimmed.charAt(i);

            // 忽略常见分隔符: 空格和短横线
            if (chr == ' ' || chr == '-')
                continue;

            if (chr == '+') {
                // '+' 只能出现在第一个字符, 且只能出现一次
                if (!firstChar)
                    throw IllegalParamException.of("手机号格式不正确");

                sb.append(chr);
                firstChar = false;
            } else if (Character.isDigit(chr)) {
                sb.append(chr);
                firstChar = false;
            } else
                // 出现任何其它字符一律视为非法
                throw IllegalParamException.of("手机号格式不正确");

        }

        String normalized = sb.toString();
        require(!normalized.isEmpty(), "手机号不能为空");
        return normalized;
    }
}
