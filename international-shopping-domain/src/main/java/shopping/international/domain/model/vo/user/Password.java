package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 密码值对象
 * 约束:
 * - 长度 8-20 字符
 * - 至少包含一个数字
 * - 至少包含一个小写字母
 * - 至少包含一个大写字母
 * - 禁止出现长度 ≥4 的连续递增字符序列 (如: 1234, abcd)
 * - 禁止出现长度 ≥4 的重复字符序列 (如: aaaa, 1111)
 */
@Getter
@EqualsAndHashCode
public final class Password {

    /**
     * 密码中至少包含的字符类型
     */
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");

    /**
     * 密码最小/最大长度
     */
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;

    /**
     * 连续/重复检测的最小长度 (4+ 不允许)
     */
    private static final int MIN_SEQUENCE_LENGTH = 4;

    /**
     * 原始密码值 (明文)
     * 通常在应用/基础设施层进行加密后再持久化
     */
    private final String value;

    /**
     * 构造方法私有化, 外部只能通过 {@link #of(String)} 创建, 确保合法性
     */
    private Password(String value) {
        this.value = value;
    }

    /**
     * 构建合法的密码值对象
     *
     * @param raw 原始密码字符串
     * @return 已通过校验的 Password
     */
    public static @NotNull Password of(String raw) {
        requireNotBlank(raw, "密码不能为空");
        String pwd = raw.trim();


        int len = pwd.length();
        if (len < MIN_LENGTH || len > MAX_LENGTH)
            throw IllegalParamException.of("密码长度必须为 " + MIN_LENGTH + "-" + MAX_LENGTH + " 个字符");

        // 至少一个数字, 小写字母, 大写字母
        if (!HAS_DIGIT.matcher(pwd).matches())
            throw IllegalParamException.of("密码必须包含至少一个数字");
        if (!HAS_LOWER.matcher(pwd).matches())
            throw IllegalParamException.of("密码必须包含至少一个小写字母");
        if (!HAS_UPPER.matcher(pwd).matches())
            throw IllegalParamException.of("密码必须包含至少一个大写字母");
        // 禁止连续字符 (如 1234, abcd), 按 ASCII 递增判断
        if (hasSequentialChars(pwd))
            throw IllegalParamException.of("密码不能包含连续字符序列 (如 1234, abcd)");
        // 禁止重复字符 (如 aaaa, 1111)
        if (hasRepeatedChars(pwd))
            throw IllegalParamException.of("密码不能包含重复字符序列 (如 aaaa, 1111)");
        return new Password(pwd);
    }

    /**
     * 检查给定字符串 <code>string</code> 是否包含长度至少为 <code>MIN_SEQUENCE_LENGTH</code> 的连续字符序列
     *
     * @param string 需要检查的字符串
     * @return 如果存在长度至少为 <code>MIN_SEQUENCE_LENGTH</code> 的连续字符, 则返回 <code>true</code>; 否则返回 <code>false</code>
     */
    private static boolean hasSequentialChars(String string) {
        if (string.length() < Password.MIN_SEQUENCE_LENGTH)
            return false;

        int run = 1;
        for (int i = 1; i < string.length(); i++) {
            if (string.charAt(i) - string.charAt(i - 1) != 1) {
                run = 1;
                continue;
            }
            run++;
            if (run >= Password.MIN_SEQUENCE_LENGTH)
                return true;
        }
        return false;
    }

    /**
     * 检查给定字符串 <code>string</code> 是否包含长度至少为 <code>MIN_SEQUENCE_LENGTH</code> 的连续重复字符序列
     *
     * @param string 需要检查的字符串
     * @return 如果存在长度至少为 <code>MIN_SEQUENCE_LENGTH</code> 的连续重复字符, 则返回 <code>true</code>; 否则返回 <code>false</code>
     */
    private static boolean hasRepeatedChars(String string) {
        if (string.length() < Password.MIN_SEQUENCE_LENGTH)
            return false;

        int run = 1;
        for (int i = 1; i < string.length(); i++) {
            if (string.charAt(i) != string.charAt(i - 1)) {
                run = 1;
                continue;
            }
            run++;
            if (run >= Password.MIN_SEQUENCE_LENGTH)
                return true;
        }
        return false;
    }

    /**
     * 避免在日志中输出明文密码
     */
    @Override
    public String toString() {
        return "Password(****)";
    }
}
