package shopping.international.infrastructure.adapter.port.user;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Email;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.port.user.IEmailPort;
import shopping.international.domain.model.vo.user.EmailAddress;
import shopping.international.domain.model.vo.user.ResendSpec;
import shopping.international.types.enums.EmailDeliveryStatus;
import shopping.international.types.exceptions.EmailSendException;
import shopping.international.types.exceptions.TooManyEmailSentException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class ResendEmailPort implements IEmailPort {
    /**
     * Redis 中 email → messageId 的默认 TTL ( ≥ 激活码 TTL, 以便轮询查询)
     */
    private static final Duration MSG_ID_TTL = Duration.ofMinutes(10);
    /**
     * 允许的同一时间内邮件最大重发次数, 超过则抛出 {@link TooManyEmailSentException}
     */
    private static final Integer MAX_REPEAT_SEND_COUNT = 3;
    /**
     * 发送邮件数超限后的冷却时间
     */
    private static final Duration MAX_REPEAT_SEND_TTL = Duration.ofMinutes(5);
    /**
     * key 前缀, 避免污染命名空间
     */
    private static final String ID_MAPPING_KEY_PREFIX = "auth:email:id-mapping:";
    /**
     * 用于构建重复发送计数键的前缀, 该键在 Redis 中用来存储特定邮箱地址尝试重发激活邮件的次数
     */
    private static final String REPEAT_SEND_COUNT_KEY_PREFIX = "auth:email:repeat-send-count:";
    /**
     * <p>用于存储 {@link ResendSpec} 实例, 该实例封装了与 Resend 邮件服务相关的配置信息</p>
     *
     * @see ResendSpec
     */
    private final ResendSpec resendSpec;
    /**
     * Redis 模板 (字符串)
     */
    private final StringRedisTemplate redisTemplate;
    /**
     * 邮件发送专用线程池
     */
    private final Executor mailExecutor;

    /**
     * 构造 <code>ResendEmailPort</code> 类的实例, 用于处理邮件重发相关的操作, 此构造器初始化了必要的属性, 包括重发规格, Redis 模板以及邮件执行器
     *
     * @param resendSpec    与 Resend 邮件服务相关的配置信息, 包含如 API 基础 URL, API 密钥等关键参数
     * @param redisTemplate 用于执行 Redis 操作的模板, 例如存储或检索激活邮件的消息 ID
     * @param mailExecutor  负责异步执行邮件发送任务的执行器, 通过限定符 <code>@Qualifier("mailExecutor")</code> 指定特定的执行器
     */
    public ResendEmailPort(ResendSpec resendSpec, StringRedisTemplate redisTemplate,
                           @Qualifier("mailExecutor") Executor mailExecutor) {
        this.resendSpec = resendSpec;
        this.redisTemplate = redisTemplate;
        this.mailExecutor = mailExecutor;
    }

    @Override
    public void sendActivationEmail(@NotNull EmailAddress email, @NotNull String code) throws EmailSendException {
        try {
            String repeatSendKey = REPEAT_SEND_COUNT_KEY_PREFIX + email.getValue();
            Long sendCount = redisTemplate.opsForValue().increment(repeatSendKey, 1L);
            if (sendCount != null && sendCount == 1L)
                redisTemplate.expire(repeatSendKey, MAX_REPEAT_SEND_TTL);
            if (sendCount != null && sendCount > MAX_REPEAT_SEND_COUNT)
                throw new TooManyEmailSentException("尝试次数过多, 请过段时间再试");
            // 立即构造请求数据, 异步执行真正发送
            String from;
            if (resendSpec.getFromName() == null || resendSpec.getFromName().isBlank())
                from = resendSpec.getFromEmail();
            else
                from = resendSpec.getFromName() + " <" + resendSpec.getFromEmail() + ">";

            final String subject = resendSpec.getActivationSubject();
            final String text = "Your verification code is: " + code + "\n\n"
                    + "If you did not request this, please ignore this email.";
            final String html = """
                    <div style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;line-height:1.6">
                      <p>Your verification code is:</p>
                      <p style="font-size:24px;font-weight:700;letter-spacing:2px;margin:12px 0">%s</p>
                      <p>If you did not request this, please ignore this email.</p>
                    </div>
                    """.formatted(code);

            // 构造发件请求
            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from(from)
                    .to(email.getValue())
                    .subject(subject)
                    .text(text)
                    .html(html)
                    .build();
            CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            Resend resend = new Resend(resendSpec.getApiKey());
                            CreateEmailResponse response = resend.emails().send(request);
                            return response == null ? null : response.getId();
                        } catch (ResendException ex) {
                            throw new EmailSendException("Resend 请求失败: " + ex.getMessage(), ex);
                        } catch (Exception ex) {
                            throw new EmailSendException("Resend 调用异常: " + ex.getMessage(), ex);
                        }
                    }, mailExecutor)
                    .whenComplete((messageId, throwable) -> {
                        if (throwable != null) {
                            log.error("异步发送激活邮件失败, email={}, requeat={}, err={}", email, request.toString(), throwable.getMessage());
                            return;
                        }
                        if (messageId == null || messageId.isBlank()) {
                            log.error("Resend 响应为空或缺少 ID, email={}, requeat={}", email, request.toString());
                            return;
                        }
                        // 写入 Redis 映射
                        bindActivationMessageId(email, messageId, MSG_ID_TTL);
                        log.info("邮件已通过 Resend 发送给 {} id={}", email, messageId);
                    });

            // 立即返回, 不阻塞
        } catch (TooManyEmailSentException e) {
            throw e;
        } catch (Exception e) {
            // 仅记录错误并返回, 让注册流程照常返回 202
            log.error("提交异步邮件发送任务失败, email={}, err={}", email, e.getMessage());
        }
    }

    /**
     * 绑定指定邮箱与激活邮件的消息 ID, 并可设置该绑定的有效期
     *
     * @param email     目标邮箱, 用于存储与其关联的激活邮件消息 ID
     * @param messageId 激活邮件的消息 ID, 将与提供的邮箱地址进行绑定
     * @param ttl       绑定的有效期, 可以为空. 如果为 null, 则表示没有过期时间; 否则, 表示从当前时刻起该绑定有效的持续时间
     */
    @Override
    public void bindActivationMessageId(@NotNull EmailAddress email, @NotNull String messageId, Duration ttl) {
        String key = ID_MAPPING_KEY_PREFIX + email.getValue();
        if (ttl == null)
            redisTemplate.opsForValue().set(key, messageId);
        else
            redisTemplate.opsForValue().set(key, messageId, ttl);
    }

    /**
     * 获取与指定邮箱关联的激活邮件的消息 ID
     *
     * @param email 目标邮箱, 用于查找与其绑定的激活邮件消息 ID
     * @return 返回与指定邮箱关联的激活邮件的消息 ID. 如果没有找到相关联的消息 ID, 则返回 null
     */
    @Override
    public String getActivationMessageId(@NotNull EmailAddress email) {
        return redisTemplate.opsForValue().get(ID_MAPPING_KEY_PREFIX + email.getValue());
    }

    /**
     * 根据消息 ID 获取邮件投递状态
     *
     * @param messageId 消息 ID, 用于从 Resend 服务查询对应的邮件状态
     * @return 邮件投递状态, 如果遇到错误或无法识别的状态, 返回 {@link EmailDeliveryStatus#UNKNOWN}
     */
    @Override
    public @NotNull EmailDeliveryStatus getStatusByMessageId(@NotNull String messageId) {
        try {
            Resend resend = new Resend(resendSpec.getApiKey());
            Email email = resend.emails().get(messageId);
            String lastEvent = email.getLastEvent(); // 某些版本字段名可能不同
            return EmailDeliveryStatus.fromString(lastEvent);
        } catch (ResendException ex) {
            log.error("Resend 状态查询失败: {}", ex.getMessage());
            return EmailDeliveryStatus.UNKNOWN;
        } catch (Exception ex) {
            log.error("Resend 状态查询异常: {}", ex.getMessage());
            return EmailDeliveryStatus.UNKNOWN;
        }
    }
}
