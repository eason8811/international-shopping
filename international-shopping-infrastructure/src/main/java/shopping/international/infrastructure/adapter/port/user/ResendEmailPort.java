package shopping.international.infrastructure.adapter.port.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import shopping.international.domain.adapter.port.user.IEmailPort;
import shopping.international.domain.model.vo.user.ResendSpec;
import shopping.international.infrastructure.gateway.user.ResendGateway;
import shopping.international.infrastructure.gateway.user.dto.ResendSendRequest;
import shopping.international.infrastructure.gateway.user.dto.ResendSendResponse;
import shopping.international.types.exceptions.EmailSendException;

import java.util.List;

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
    /**
     * <p>用于与 Resend 服务进行交互的网关接口实例, 通过这个实例, 可以发送邮件到指定的收件人</p>
     *
     * @see ResendGateway
     */
    private final ResendGateway resendGateway;

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

            ResendSendRequest body = ResendSendRequest.builder()
                    .from(from)
                    .to(List.of(email))
                    .subject(subject)
                    .html(html)
                    .text(text)
                    .build();

            String url = ensureNoTrailingSlash(resendSpec.getBaseUrl()) + "/emails";
            String auth = "Bearer " + resendSpec.getApiKey();

            Response<ResendSendResponse> resp = resendGateway.send(url, auth, body).execute();

            if (!resp.isSuccessful())
                try (ResponseBody err = resp.errorBody()) {
                    String msg = (err != null) ? err.string() : "null";
                    throw new EmailSendException("Resend 请求失败: http=" + resp.code() + ", msg=" + msg);
                }


            ResendSendResponse data = resp.body();
            if (data == null || data.getId() == null || data.getId().isBlank())
                throw new EmailSendException("Resend 响应为空或缺少ID");


            log.info("Mail sent to {} via Resend. id={}", email, data.getId());
        } catch (EmailSendException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailSendException("Resend 调用异常: " + e.getMessage());
        }
    }

    /**
     * 确保 URL 末尾没有斜杠
     *
     * @param url URL
     * @return URL 末尾没有斜杠的 URL
     */
    private static String ensureNoTrailingSlash(String url) {
        if (url == null)
            return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
