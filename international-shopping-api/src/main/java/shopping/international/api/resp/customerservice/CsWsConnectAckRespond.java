package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * WebSocket 建连确认响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsConnectAckRespond {
    /**
     * 连接 ID
     */
    @NotNull
    private String connectionId;
    /**
     * 服务端时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime serverTime;
    /**
     * 心跳间隔（秒）
     */
    @NotNull
    private Integer heartbeatIntervalSeconds;
    /**
     * 续传窗口时长（秒）
     */
    @NotNull
    private Integer resumeTtlSeconds;
}
