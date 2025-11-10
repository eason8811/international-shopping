package shopping.international.api.req.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.vo.user.EmailAddress;

import static shopping.international.types.utils.FieldValidateUtils.requireIsEmail;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 验证邮箱并激活账户的请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailRequest {
    /**
     * 需要激活的邮箱
     */
    private String email;
    /**
     * 邮箱验证码
     */
    private String code;

    public void validate() {
        requireIsEmail(email, "邮箱格式错误");
        requireNotBlank(code, "验证码不能为空");
    }

    /**
     * 将当前请求中的邮箱地址转换为 {@link EmailAddress} 值对象
     *
     * @return 转换后的 {@link EmailAddress} 实例
     */
    public EmailAddress toEmailVO() {
        return EmailAddress.of(email);
    }
}
