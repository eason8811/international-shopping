package shopping.international.infrastructure.adapter.port.shipping;

import lombok.RequiredArgsConstructor;
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
import java.util.HexFormat;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 17Track 端口实现, 基于 Retrofit 和 Redis
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SeventeenTrackProperties.class)
public class SeventeenTrackPort implements ISeventeenTrackPort {

    /**
     * 注册运单路径
     */
    private static final String REGISTER_PATH = "/track/v2/register";

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

        String url = properties.getBaseUrl() + REGISTER_PATH;
        List<SeventeenTrackRegisterTrackRequest> request = List.of(
                SeventeenTrackRegisterTrackRequest.builder()
                        .number(command.trackingNo())
                        .carrier(command.carrierCode())
                        .build()
        );

        SeventeenTrackRegisterTrackRespond respond = executeOrThrow(
                api.registerTrack(url, properties.getToken(), request),
                "17Track 注册运单失败"
        );

        if (respond.getCode() == null || respond.getData() == null
                || respond.getData().getAccepted() == null || respond.getData().getAccepted().isEmpty())
            throw new ConflictException("17Track 注册运单失败, code: " + respond.getCode());

        if (respond.getCode() != 0 || (respond.getData().getRejected() != null && !respond.getData().getRejected().isEmpty())) {
            String errorMessage = respond.getData().getRejected() == null ? null : respond.getData().getRejected().get(0).getError().getMessage();
            throw new ConflictException("17Track 注册运单失败, code: " + respond.getCode() + ", message: " + errorMessage);
        }
    }

    /**
     * WebHook 验签并防重放
     *
     * @param command 验签命令
     */
    @Override
    public void verifyWebhookAndReplayProtection(@NotNull VerifyWebhookCommand command) {
        requireNotNull(command, "command 不能为空");
        requireNotBlank(command.sign(), "sign 不能为空");
        requireNotBlank(command.rawBody(), "rawBody 不能为空");
        requireNotBlank(command.dedupeKey(), "dedupeKey 不能为空");
        requireNotBlank(properties.getWebhookKey(), "seventeen-track.webhook-key 未配置");

        String expected = sha256Hex(command.rawBody() + "/" + properties.getWebhookKey());
        if (!expected.equalsIgnoreCase(command.sign().strip()))
            throw new IllegalParamException("17Track WebHook 验签失败");

        boolean first = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(
                        command.dedupeKey(),
                        "1",
                        command.replayTtl()
                )
        );
        if (!first)
            throw new ConflictException("17Track WebHook 重复事件");
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
