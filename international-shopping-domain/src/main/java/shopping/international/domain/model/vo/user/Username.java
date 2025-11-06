package shopping.international.domain.model.vo.user;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 用户名 (登录名) 值对象
 * <p>不可变, 持有规范化后的用户名 (去首尾空格)</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class Username {
    /**
     * 用户名正则表达式
     */
    private static final Pattern PATTERN = Pattern.compile("^(?=.*[^0-9])[A-Za-z0-9_-]{3,64}$");

    /**
     * 规范化后的用户名
     */
    private final String value;

    /**
     * 构造方法, 私有化, 不允许外部创建实例
     *
     * @param value 用户名
     */
    private Username(String value) {
        this.value = value;
    }

    /**
     * 工厂方法, 创建用户名
     *
     * @param raw 原始输入
     * @return 值对象
     */
    public static Username of(String raw) {
        requireNotNull(raw, "用户名不能为空");
        String val = raw.trim();
        require(PATTERN.matcher(val).matches(), "用户名必须是3-64位字母数字下划线或 (_) 连字符 (-)");
        return new Username(val);
    }
}
