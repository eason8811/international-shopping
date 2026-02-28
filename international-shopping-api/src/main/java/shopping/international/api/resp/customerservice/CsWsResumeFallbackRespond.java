package shopping.international.api.resp.customerservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WebSocket 续传降级建议响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsResumeFallbackRespond {
    /**
     * 降级策略
     */
    @NotNull
    private String strategy;
    /**
     * 建议用于 HTTP 增量补偿的 after_id
     */
    @Nullable
    private Long suggestAfterId;
    /**
     * 建议重试等待秒数
     */
    @Nullable
    private Integer retryAfterSeconds;
}
