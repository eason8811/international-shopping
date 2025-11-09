package shopping.international.api.req.user;

import lombok.Data;
import shopping.international.domain.model.vo.user.EmailAddress;

import static shopping.international.types.utils.FieldValidateUtils.requireIsEmail;

/**
 * 申请变更邮箱, 向新邮箱发送验证码
 */
@Data
public class ChangeEmailRequest {
    /**
     * 新邮箱
     */
    private String newEmail;

    /**
     * 校验与规范化
     */
    public void validate() {
        requireIsEmail(newEmail, "邮箱不能为空");
    }

    /**
     * 转为领域值对象
     */
    public EmailAddress toEmailVO() {
        return EmailAddress.of(newEmail);
    }
}
