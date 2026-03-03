package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.WsEventType;

import java.util.List;

/**
 * 用户侧 WebSocket 会话创建命令值对象
 *
 * @param ticketNos    订阅工单号列表
 * @param ticketIds    订阅工单 ID 列表
 * @param eventTypes   订阅事件类型列表
 * @param lastEventId  续传锚点事件 ID
 */
public record TicketWsSessionCreateCommand(List<String> ticketNos,
                                           List<Long> ticketIds,
                                           List<WsEventType> eventTypes,
                                           @Nullable String lastEventId) {

    /**
     * 规范化构造, 避免可选集合字段为 null
     */
    public TicketWsSessionCreateCommand {
        ticketNos = ticketNos == null ? List.of() : List.copyOf(ticketNos);
        ticketIds = ticketIds == null ? List.of() : List.copyOf(ticketIds);
        eventTypes = eventTypes == null ? List.of() : List.copyOf(eventTypes);
    }
}

