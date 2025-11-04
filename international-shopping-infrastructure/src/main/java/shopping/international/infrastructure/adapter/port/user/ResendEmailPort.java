package shopping.international.infrastructure.adapter.port.user;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.user.IEmailPort;
import shopping.international.domain.model.vo.user.ResendSpec;
import shopping.international.types.exceptions.EmailSendException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mail.provider", havingValue = "resend")
public class ResendEmailPort implements IEmailPort {

    /**
     * <p>用于存储 {@link ResendSpec} 实例，该实例封装了与 Resend 邮件服务相关的配置信息</p>
     *
     * @see ResendSpec
     */
    private final ResendSpec resendSpec;

    @Override
    public void sendActivationEmail(@NotNull String email, @NotNull String code) throws EmailSendException {
        try {
            String from;
            if (resendSpec.getFromName() == null || resendSpec.getFromName().isBlank())
                from = resendSpec.getFromEmail();
            else
                from = resendSpec.getFromName() + " <" + resendSpec.getFromEmail() + ">";
            String subject = resendSpec.getActivationSubject();

            String text = "Your verification code is: " + code + "\n\n"
                    + "If you did not request this, please ignore this email.";
            String html = """
                    <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;line-height:1.6">
                      <p>Your verification code is:</p>
                      <p style="font-size:24px;font-weight:700;letter-spacing:2px;margin:12px 0">%s</p>
                      <p>If you did not request this, please ignore this email.</p>
                    </div>
                    """.formatted(code);

            Resend resend = new Resend(resendSpec.getApiKey());

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from(from)
                    .to(email)
                    .subject(subject)
                    .text(text)
                    .html(html)
                    .build();

            CreateEmailResponse response = resend.emails().send(request);
            if (response.getId() == null || response.getId().isBlank())
                throw new EmailSendException("Resend 响应为空或缺少ID");
            log.info("邮件已通过 Resend 发送给 {} id={}", email, response.getId());
        } catch (ResendException e) {
            throw new EmailSendException("Resend 请求失败: " + e.getMessage());
        } catch (EmailSendException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailSendException("Resend 调用异常: " + e.getMessage());
        }
    }
}
