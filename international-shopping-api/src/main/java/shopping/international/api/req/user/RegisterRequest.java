package shopping.international.api.req.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.regex.Pattern;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 注册请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    /**
     * 用户名 (唯一登录名)
     */
    private String username;
    /**
     * 登录密码 (明文, 应用服务内会进行哈希)
     */
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 手机 (可选, 格式校验由应用/领域层处理)
     */
    private String phone;

    /**
     * 匹配邮箱格式正则表达式
     * 此模式主要用于 {@link RegisterRequest#validate()} 方法中, 匹配邮箱格式
     */
    private static final Pattern emailPattern = Pattern.compile("\\w+@\\w+(\\.\\w+)+");

    /**
     * 验证请求 DTO 字段是否合法
     *
     * @throws IllegalParamException 如果任一被验证的字段不符合预期条件
     */
    public void validate() {
        requireNotBlank(username, "用户名不能为空");
        requireNotBlank(password, "密码不能为空");
        requireNotBlank(nickname, "昵称不能为空");
        requireNotBlank(email, "邮箱不能为空");
        require(emailPattern.matcher(email).matches(), "邮箱格式不正确");
    }
}
