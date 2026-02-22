package shopping.international.infrastructure.adapter.port.shipping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;
import shopping.international.domain.adapter.port.shipping.ISeventeenTrackPort;
import shopping.international.infrastructure.gateway.shipping.SeventeenTrackApi;
import shopping.international.infrastructure.gateway.shipping.dto.SeventeenTrackRegisterTrackRequest;
import shopping.international.infrastructure.gateway.shipping.dto.SeventeenTrackRegisterTrackRespond;
import shopping.international.types.config.SeventeenTrackProperties;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 17Track 端口实现, 基于 Retrofit 和 Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SeventeenTrackProperties.class)
public class SeventeenTrackPort implements ISeventeenTrackPort {

    /**
     * 注册运单路径
     */
    private static final String REGISTER_PATH = "/track/v2/register";
    /**
     * WebHook 处理态键后缀
     */
    private static final String WEBHOOK_PROCESSING_SUFFIX = ":processing";
    /**
     * 运单注册完成键前缀
     */
    private static final String REGISTER_DONE_PREFIX = "shipping:17track:register:done:";
    /**
     * 运单注册处理态键后缀
     */
    private static final String REGISTER_PROCESSING_SUFFIX = ":processing";
    /**
     * 运单注册完成态 TTL
     */
    private static final Duration REGISTER_DONE_TTL = Duration.ofDays(30);
    /**
     * 运单注册处理态 TTL
     */
    private static final Duration REGISTER_PROCESSING_TTL = Duration.ofMinutes(2);
    /**
     * WebHook 处理态默认 TTL
     */
    private static final Duration WEBHOOK_PROCESSING_TTL = Duration.ofMinutes(5);

    /**
     * 17Track API
     */
    private final SeventeenTrackApi api;
    /**
     * Redis 模板
     */
    private final StringRedisTemplate redisTemplate;
    /**
     * 配置
     */
    private final SeventeenTrackProperties properties;

    /**
     * 向 17Track 注册运单
     *
     * @param command 注册命令
     */
    @Override
    public void registerTracking(@NotNull RegisterTrackingCommand command) {
        requireNotNull(command, "command 不能为空");
        requireNotBlank(command.trackingNo(), "trackingNo 不能为空");
        requireNotBlank(command.idempotencyKey(), "idempotencyKey 不能为空");
        requireNotBlank(properties.getBaseUrl(), "seventeen-track.base-url 未配置");
        requireNotBlank(properties.getToken(), "seventeen-track.token 未配置");

        String doneKey = REGISTER_DONE_PREFIX + sha256Hex(command.idempotencyKey());
        String processingKey = doneKey + REGISTER_PROCESSING_SUFFIX;
        if (hasRedisKey(doneKey))
            return;

        boolean acquired = tryAcquire(processingKey, REGISTER_PROCESSING_TTL);
        if (!acquired) {
            if (hasRedisKey(doneKey))
                return;
            if (hasRedisKey(processingKey)) {
                log.warn("17Track 注册运单处理中...");
                return;
            }
            throw new ConflictException("17Track 注册运单抢占失败");
        }

        String url = properties.getBaseUrl() + REGISTER_PATH;
        List<SeventeenTrackRegisterTrackRequest> request = List.of(
                SeventeenTrackRegisterTrackRequest.builder()
                        .number(command.trackingNo())
                        .carrier(command.carrierCode())
                        .build()
        );

        try {
            SeventeenTrackRegisterTrackRespond respond = executeOrThrow(
                    api.registerTrack(url, properties.getToken(), request),
                    "17Track 注册运单失败"
            );

            if (isRegisterAccepted(respond) || isAlreadyRegistered(respond)) {
                markDone(doneKey, REGISTER_DONE_TTL);
                return;
            }

            throw new ConflictException("17Track 注册运单失败, code: " + respond.getCode() + ", message: " + firstRejectMessage(respond));
        } finally {
            redisTemplate.delete(processingKey);
        }
    }

    /**
     * WebHook 验签并尝试进入处理态
     *
     * @param command 验签命令
     * @return 门禁结果
     */
    @Override
    public @NotNull WebhookGateResult verifyWebhookAndTryEnterProcessing(@NotNull VerifyWebhookCommand command) {
        requireNotNull(command, "command 不能为空");
        requireNotBlank(command.sign(), "sign 不能为空");
        requireNotBlank(command.rawBody(), "rawBody 不能为空");
        requireNotBlank(command.dedupeKey(), "dedupeKey 不能为空");
        requireNotNull(command.replayTtl(), "replayTtl 不能为空");
        requireNotBlank(properties.getWebhookKey(), "seventeen-track.webhook-key 未配置");

        String expected = sha256Hex(command.rawBody() + "/" + properties.getWebhookKey());
        if (!expected.equalsIgnoreCase(command.sign().strip()))
            throw new IllegalParamException("17Track WebHook 验签失败");

        String doneKey = command.dedupeKey();
        if (hasRedisKey(doneKey))
            return WebhookGateResult.ALREADY_PROCESSED;

        String processingKey = webhookProcessingKey(command.dedupeKey());
        Duration processingTtl = resolveWebhookProcessingTtl(command.replayTtl());
        boolean acquired = tryAcquire(processingKey, processingTtl);
        if (acquired)
            return WebhookGateResult.SHOULD_PROCESS;
        if (hasRedisKey(doneKey))
            return WebhookGateResult.ALREADY_PROCESSED;
        return WebhookGateResult.PROCESSING;
    }

    /**
     * 标记 WebHook 已处理
     *
     * @param dedupeKey 去重键
     * @param doneTtl 完成态 TTL
     */
    @Override
    public void markWebhookProcessed(@NotNull String dedupeKey,
                                     @NotNull Duration doneTtl) {
        requireNotBlank(dedupeKey, "dedupeKey 不能为空");
        requireNotNull(doneTtl, "doneTtl 不能为空");
        markDone(dedupeKey, doneTtl);
        redisTemplate.delete(webhookProcessingKey(dedupeKey));
    }

    /**
     * 清理 WebHook 处理态
     *
     * @param dedupeKey 去重键
     */
    @Override
    public void clearWebhookProcessing(@NotNull String dedupeKey) {
        requireNotBlank(dedupeKey, "dedupeKey 不能为空");
        redisTemplate.delete(webhookProcessingKey(dedupeKey));
    }

    /**
     * 执行 Retrofit 调用, 并在失败时抛出异常
     *
     * @param call    调用句柄
     * @param message 失败信息
     * @return 响应体
     */
    private <T> @NotNull T executeOrThrow(@NotNull Call<T> call,
                                          @NotNull String message) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful() && response.body() != null)
                return response.body();

            String error = null;
            try (ResponseBody body = response.errorBody()) {
                if (body != null)
                    error = body.string();
                throw new IllegalParamException(message + ", http 代码: " + response.code() + ", 错误响应体: " + error);
            }
        } catch (ConflictException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ConflictException(message + ", " + exception.getMessage());
        }
    }

    /**
     * 判断注册响应是否为成功
     *
     * @param respond 注册响应
     * @return true 表示注册成功
     */
    private boolean isRegisterAccepted(@NotNull SeventeenTrackRegisterTrackRespond respond) {
        if (respond.getCode() == null || respond.getData() == null)
            return false;
        boolean accepted = respond.getData().getAccepted() != null && !respond.getData().getAccepted().isEmpty();
        boolean rejected = respond.getData().getRejected() != null && !respond.getData().getRejected().isEmpty();
        return respond.getCode() == 0 && accepted && !rejected;
    }

    /**
     * 判断注册响应是否为“已注册”的幂等成功
     *
     * @param respond 注册响应
     * @return true 表示可按幂等成功处理
     */
    private boolean isAlreadyRegistered(@NotNull SeventeenTrackRegisterTrackRespond respond) {
        if (respond.getData() == null || respond.getData().getRejected() == null || respond.getData().getRejected().isEmpty())
            return false;
        return respond.getData().getRejected().stream().allMatch(this::isAlreadyRegisteredItem);
    }

    /**
     * 判断拒绝项是否表达“已注册”
     *
     * @param item 拒绝项
     * @return true 表示已注册
     */
    private boolean isAlreadyRegisteredItem(@NotNull SeventeenTrackRegisterTrackRespond.RejectedItem item) {
        if (item.getError() == null || item.getError().getMessage() == null)
            return false;
        String message = item.getError().getMessage().strip().toLowerCase(Locale.ROOT);
        return message.contains("already")
                || message.contains("exists")
                || message.contains("duplicate")
                || message.contains("registered");
    }

    /**
     * 提取首个拒绝项错误信息
     *
     * @param respond 注册响应
     * @return 错误信息
     */
    private String firstRejectMessage(@NotNull SeventeenTrackRegisterTrackRespond respond) {
        if (respond.getData() == null || respond.getData().getRejected() == null || respond.getData().getRejected().isEmpty())
            return "";
        SeventeenTrackRegisterTrackRespond.RejectedItem first = respond.getData().getRejected().get(0);
        if (first == null || first.getError() == null || first.getError().getMessage() == null)
            return "";
        return first.getError().getMessage();
    }

    /**
     * 计算 WebHook 处理态键
     *
     * @param dedupeKey 去重键
     * @return 处理态键
     */
    private String webhookProcessingKey(@NotNull String dedupeKey) {
        return dedupeKey + WEBHOOK_PROCESSING_SUFFIX;
    }

    /**
     * 判断 Redis 中是否存在指定键
     *
     * @param key 键
     * @return true 表示存在
     */
    private boolean hasRedisKey(@NotNull String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 尝试抢占处理权
     *
     * @param key 锁键
     * @param ttl 锁 TTL
     * @return true 表示抢占成功
     */
    private boolean tryAcquire(@NotNull String key,
                               @NotNull Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
    }

    /**
     * 标记事件已处理完成
     *
     * @param doneKey 完成态键
     * @param doneTtl 完成态 TTL
     */
    private void markDone(@NotNull String doneKey,
                          @NotNull Duration doneTtl) {
        redisTemplate.opsForValue().set(doneKey, "1", doneTtl);
    }

    /**
     * 计算 WebHook 处理态 TTL
     *
     * @param replayTtl 重放保护 TTL
     * @return 处理态 TTL
     */
    private Duration resolveWebhookProcessingTtl(@NotNull Duration replayTtl) {
        if (replayTtl.isNegative() || replayTtl.isZero())
            return WEBHOOK_PROCESSING_TTL;
        return replayTtl.compareTo(WEBHOOK_PROCESSING_TTL) < 0 ? replayTtl : WEBHOOK_PROCESSING_TTL;
    }

    /**
     * 计算 SHA256
     *
     * @param raw 原文
     * @return 十六进制字符串
     */
    private static @NotNull String sha256Hex(@NotNull String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception exception) {
            throw new IllegalStateException("计算 SHA256 失败, " + exception.getMessage(), exception);
        }
    }
}
