package shopping.international.domain.adapter.port.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * 17Track 网关端口接口, 用于注册运单, WebHook 验签和去重
 */
public interface ISeventeenTrackPort {

    /**
     * WebHook 事件处理门禁结果
     */
    enum WebhookGateResult {
        /**
         * 首次事件, 当前请求应继续处理业务
         */
        SHOULD_PROCESS,
        /**
         * 事件已处理完成, 当前请求应直接幂等成功返回
         */
        ALREADY_PROCESSED,
        /**
         * 事件正在被其他请求处理, 当前请求应等待重试
         */
        PROCESSING
    }

    /**
     * 向 17Track 注册运单
     *
     * @param command 注册命令
     */
    void registerTracking(@NotNull RegisterTrackingCommand command);

    /**
     * WebHook 验签并尝试进入处理态
     *
     * @param command 验签命令
     * @return 门禁结果
     */
    @NotNull
    WebhookGateResult verifyWebhookAndTryEnterProcessing(@NotNull VerifyWebhookCommand command);

    /**
     * 标记 WebHook 事件处理完成
     *
     * @param dedupeKey 去重键
     * @param doneTtl 完成态 TTL
     */
    void markWebhookProcessed(@NotNull String dedupeKey,
                              @NotNull Duration doneTtl);

    /**
     * 清理 WebHook 处理态
     *
     * @param dedupeKey 去重键
     */
    void clearWebhookProcessing(@NotNull String dedupeKey);

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
