package shopping.international.api.req.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireIsEmail;

/**
 * 重新发送激活邮件的请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendActivationRequest {
    /**
     * 注册邮箱
     */
    private String email;

    /**
     * 验证当前请求中的邮箱地址是否有效
     *
     * @throws IllegalParamException 如果 <code>email</code> 为 <code>null</code>, 空白或不符合电子邮件格式
     */
    public void validate() {
        requireIsEmail(email, "邮箱格式不正确");
    }
}
