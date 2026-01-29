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
     * 手机国家码
     */
    private String countryCode;
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
        if (countryCode != null) {
            requireNotBlank(countryCode, "电话国家码不能为空");
            require(countryCode.matches("^[1-9][0-9]{0,2}$"), "电话国家代码格式不正确");
            require(account.matches("^[0-9]{1,14}$"), "电话号码格式不正确");
        }
    }
}
