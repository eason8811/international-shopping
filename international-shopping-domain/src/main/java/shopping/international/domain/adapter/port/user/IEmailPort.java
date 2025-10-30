package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.types.exceptions.EmailSendException;

/**
 * 邮件发送端口 (Port), 领域层通过该端口向外部邮件系统 (如 SendGrid) 发送验证码邮件
 */
public interface IEmailPort {

    /**
     * 发送激活验证码邮件
     *
     * @param email 收件人邮箱
     * @param code  激活验证码
     * @throws EmailSendException 邮件系统异常或发送失败时抛出
     */
    void sendActivationEmail(@NotNull String email, @NotNull String code) throws EmailSendException;
}
