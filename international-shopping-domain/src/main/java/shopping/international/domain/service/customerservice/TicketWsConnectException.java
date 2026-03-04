package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.WsErrorCode;
import shopping.international.domain.model.enums.customerservice.WsResumeFallbackStrategy;

/**
 * WebSocket 建连失败异常, 用于向 trigger 层传递错误码与降级建议
 */
public class TicketWsConnectException extends RuntimeException {

    /**
     * 协议错误码
     */
    @NotNull
    private final WsErrorCode code;
    /**
     * 续传降级策略, 可为空
     */
    @Nullable
    private final WsResumeFallbackStrategy fallbackStrategy;
    /**
     * 推荐用于 HTTP 增量补偿的 after_id, 可为空
     */
    @Nullable
    private final Long suggestAfterId;
    /**
     * 建议重试等待秒数, 可为空
     */
    @Nullable
    private final Integer retryAfterSeconds;

    /**
     * 创建建连失败异常
     *
     * @param code              协议错误码
     * @param message           错误消息
     * @param fallbackStrategy  续传降级策略, 可为空
     * @param suggestAfterId    推荐补偿锚点, 可为空
     * @param retryAfterSeconds 建议重试等待秒数, 可为空
     */
    public TicketWsConnectException(@NotNull WsErrorCode code,
                                    @NotNull String message,
                                    @Nullable WsResumeFallbackStrategy fallbackStrategy,
                                    @Nullable Long suggestAfterId,
                                    @Nullable Integer retryAfterSeconds) {
        super(message);
        this.code = code;
        this.fallbackStrategy = fallbackStrategy;
        this.suggestAfterId = suggestAfterId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * 获取协议错误码
     *
     * @return 错误码
     */
    public @NotNull WsErrorCode getCode() {
        return code;
    }

    /**
     * 获取续传降级策略
     *
     * @return 续传降级策略, 可为空
     */
    public @Nullable WsResumeFallbackStrategy getFallbackStrategy() {
        return fallbackStrategy;
    }

    /**
     * 获取推荐补偿锚点
     *
     * @return 推荐补偿锚点, 可为空
     */
    public @Nullable Long getSuggestAfterId() {
        return suggestAfterId;
    }

    /**
     * 获取建议重试等待秒数
     *
     * @return 建议重试等待秒数, 可为空
     */
    public @Nullable Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
