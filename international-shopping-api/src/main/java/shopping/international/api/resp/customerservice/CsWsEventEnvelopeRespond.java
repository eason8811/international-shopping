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
 * WebSocket 统一事件包裹响应对象
 *
 * @param <T> 事件响应的数据负载类型, 可为: {@code CsWsConnectAckRespond, TicketMessageRespond, CsWsTicketReadUpdatedEventDataRespond, TicketStatusLogRespond, TicketAssignmentLogRespond}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsEventEnvelopeRespond<T> {
    /**
     * 事件唯一 ID
     */
    @NotNull
    private String eventId;
    /**
     * 单连接内递增序号
     */
    @NotNull
    private Long seq;
    /**
     * 事件类型
     */
    @NotNull
    private String eventType;
    /**
     * 事件发生时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
    /**
     * 工单 ID
     */
    @Nullable
    private Long ticketId;
    /**
     * 工单编号
     */
    @Nullable
    private String ticketNo;
    /**
     * 链路追踪 ID
     */
    @Nullable
    private String traceId;
    /**
     * 事件负载对象
     */
    @NotNull
    private T data;
}
