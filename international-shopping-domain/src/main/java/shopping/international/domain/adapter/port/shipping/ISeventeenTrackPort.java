package shopping.international.domain.adapter.port.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * 17Track 网关端口接口, 用于注册运单, WebHook 验签和去重
 */
public interface ISeventeenTrackPort {

    /**
     * 向 17Track 注册运单
     *
     * @param command 注册命令
     */
    void registerTracking(@NotNull RegisterTrackingCommand command);

    /**
     * WebHook 验签并防重放
     *
     * @param command 验签命令
     */
    void verifyWebhookAndReplayProtection(@NotNull VerifyWebhookCommand command);

    /**
     * 17Track 注册运单命令
     *
     * @param trackingNo     追踪号
     * @param carrierCode    承运商编码
     * @param idempotencyKey 幂等键
     */
    record RegisterTrackingCommand(@NotNull String trackingNo,
                                   @Nullable String carrierCode,
                                   @NotNull String idempotencyKey) {
    }

    /**
     * 17Track WebHook 验签命令
     *
     * @param sign      Header sign
     * @param rawBody   原始请求体
     * @param dedupeKey 去重键
     * @param replayTtl 重放保护 TTL
     */
    record VerifyWebhookCommand(@NotNull String sign,
                                @NotNull String rawBody,
                                @NotNull String dedupeKey,
                                @NotNull Duration replayTtl) {
    }
}
