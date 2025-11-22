package shopping.international.api.req.user;

import lombok.Data;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 找回密码 - 重置密码 请求体
 */
@Data
public class ResetPasswordRequest {
    /**
     * 账号 (用户名 / 邮箱 / 手机号)
     */
    private String account;
    /**
     * 邮件验证码
     */
    private String code;
    /**
     * 新密码 (明文)
     */
    private String newPassword;

    public void validate() {
        requireNotBlank(account, "账号不能为空");
        requireNotBlank(code, "验证码不能为空");
        requireNotBlank(newPassword, "新密码不能为空");
        require(newPassword.length() >= 6, "密码长度至少6位");
    }
}
