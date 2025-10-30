package shopping.international.api.req.user;

import lombok.Data;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 本地登录请求体
 */
@Data
public class LoginRequest {
    /**
     * 账号 (用户名 / 邮箱 / 手机号)
     */
    private String account;
    /**
     * 登录密码
     */
    private String password;

    public void validate() {
        requireNotBlank(account, "账号不能为空");
        requireNotBlank(password, "密码不能为空");
    }
}
