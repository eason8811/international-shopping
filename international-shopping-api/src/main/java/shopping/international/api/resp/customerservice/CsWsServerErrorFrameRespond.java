package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * WebSocket 服务端错误帧响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsServerErrorFrameRespond {
    /**
     * 帧类型，固定为 error
     */
    @NotNull
    private String type;
    /**
     * 错误码
     */
    @NotNull
    private String code;
    /**
     * 错误消息
     */
    @NotNull
    private String message;
    /**
     * 发生时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
    /**
     * 服务端确认的最后可续传事件 ID
     */
    @Nullable
    private String lastEventId;
    /**
     * 续传降级建议
     */
    @Nullable
    private CsWsResumeFallbackRespond fallback;
}
