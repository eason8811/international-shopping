package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * WebSocket 会话签发数据响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsSessionIssueDataRespond {
    /**
     * WebSocket 短期访问令牌
     */
    @NotNull
    private String wsToken;
    /**
     * WebSocket 连接地址
     */
    @NotNull
    private String wsUrl;
    /**
     * 签发时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;
    /**
     * 过期时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;
    /**
     * 心跳间隔 (秒)
     */
    @NotNull
    private Integer heartbeatIntervalSeconds;
    /**
     * 续传窗口时长 (秒)
     */
    @NotNull
    private Integer resumeTtlSeconds;
}
