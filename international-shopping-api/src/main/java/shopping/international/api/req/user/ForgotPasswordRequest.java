package shopping.international.api.req.user;

import lombok.Data;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 找回密码 - 发送验证码 请求体
 */
@Data
public class ForgotPasswordRequest {
    /**
     * 账号 (用户名 / 邮箱 / 手机号)
     */
    private String account;

    public void validate() {
        requireNotBlank(account, "账号不能为空");
    }
}
