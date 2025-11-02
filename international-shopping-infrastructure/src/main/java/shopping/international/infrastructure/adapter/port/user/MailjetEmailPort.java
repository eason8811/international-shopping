package shopping.international.infrastructure.adapter.port.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import retrofit2.Response;
import retrofit2.Retrofit;
import shopping.international.app.config.MailjetProperties;
import shopping.international.app.config.RetrofitConfig.RetrofitFactory;
import shopping.international.domain.adapter.port.user.IEmailPort;
import shopping.international.infrastructure.gateway.user.MailjetGateway;
import shopping.international.infrastructure.gateway.user.dto.MailjetSendRequest;
import shopping.international.infrastructure.gateway.user.dto.MailjetSendRequest.MailjetEmail;
import shopping.international.infrastructure.gateway.user.dto.MailjetSendRequest.MailjetMessage;
import shopping.international.infrastructure.gateway.user.dto.MailjetSendResponse;
import shopping.international.types.exceptions.EmailSendException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Mailjet API 的邮件发送适配器
 *
 * <p>实现 {@link IEmailPort}，用于发送激活验证码邮件</p>
 * <p>内部使用 Retrofit + OkHttp 调用 Mailjet v3.1 /send 接口</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(MailjetProperties.class)
public class MailjetEmailPort implements IEmailPort {

    /**
     * Mailjet API 配置
     *
     * @see MailjetProperties
     */
    private final MailjetProperties props;
    /**
     * Retrofit 工厂
     */
    private final RetrofitFactory retrofitFactory;

    /**
     * 发送激活验证码邮件
     *
     * <p>使用 Mailjet Basic Auth: {@code Basic base64(apiKey:apiSecret)}</p>
     *
     * @param email 收件人邮箱
     * @param code  激活验证码
     * @throws EmailSendException 发送失败或网关异常时抛出
     */
    @Override
    public void sendActivationEmail(@NotNull String email, @NotNull String code) throws EmailSendException {
        try {
            // 1. 构建请求体
            MailjetEmail from = MailjetEmail.builder()
                    .email(props.getFromEmail())
                    .name(props.getFromName())
                    .build();
            MailjetEmail to = MailjetEmail.builder()
                    .email(email)
                    .name(email)
                    .build();

            String subject = props.getActivationSubject();
            String text = "Your verification code is: " + code + "\n\n"
                    + "If you did not request this, please ignore this email.";
            String html = """
                    <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;line-height:1.6">
                      <p>Your verification code is:</p>
                      <p style="font-size:24px;font-weight:700;letter-spacing:2px;margin:12px 0">%s</p>
                      <p>If you did not request this, please ignore this email.</p>
                    </div>
                    """.formatted(code);

            MailjetMessage message = MailjetMessage.builder()
                    .from(from)
                    .to(List.of(to))
                    .subject(subject)
                    .textPart(text)
                    .htmlPart(html)
                    .customId("activation-" + UUID.randomUUID())
                    .build();

            MailjetSendRequest body = MailjetSendRequest.builder()
                    .messages(List.of(message))
                    .build();

            // 2. 构建网关并调用
            Retrofit retrofit = retrofitFactory.create(props.getBaseUrl());
            MailjetGateway gateway = retrofit.create(MailjetGateway.class);

            String auth = basicAuth(props.getApiKey(), props.getApiSecret());

            Response<MailjetSendResponse> resp = gateway.send(auth, body).execute();

            // 3. 解析响应
            if (!resp.isSuccessful())
                try (ResponseBody errorBody = resp.errorBody()) {
                    String err = "http=" + resp.code() + ", msg=" + (errorBody != null ? errorBody.string() : "null");
                    throw new EmailSendException("Mailjet 请求失败: " + err);
                }

            MailjetSendResponse response = resp.body();
            if (response == null || response.getMessages() == null || response.getMessages().isEmpty())
                throw new EmailSendException("Mailjet 响应为空或不完整");

            boolean allSuccess = response.getMessages().stream()
                    .allMatch(msg -> "success".equalsIgnoreCase(msg.getStatus()));
            if (!allSuccess)
                throw new EmailSendException("Mailjet 发送失败: " + response);

            log.info("Mail sent to {} via Mailjet.", email);
        } catch (EmailSendException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailSendException("Mailjet 调用异常: " + e.getMessage());
        }
    }

    /**
     * 生成 Basic Auth 头部值, 格式为 {@code Basic base64(apiKey:apiSecret)}
     *
     * @param apiKey    API密钥
     * @param apiSecret API密钥密码
     * @return Base64编码后的Basic认证字符串
     */
    private static String basicAuth(String apiKey, String apiSecret) {
        String raw = apiKey + ":" + apiSecret;
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }
}
