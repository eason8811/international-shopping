package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.api.resp.customerservice.CsWsSessionIssueDataRespond;
import shopping.international.api.resp.customerservice.CsWsTicketReadUpdatedEventDataRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.domain.model.vo.customerservice.TicketMessageView;
import shopping.international.domain.model.vo.customerservice.TicketReadUpdateView;
import shopping.international.domain.model.vo.customerservice.TicketWsSessionIssueView;

/**
 * 工单领域公共响应装配器, 统一管理用户侧和管理侧复用的 DTO 映射逻辑
 */
public final class TicketCommonRespondAssembler {

    /**
     * 私有构造方法, 工具类不允许实例化
     */
    private TicketCommonRespondAssembler() {
    }

    /**
     * 消息视图转换为响应 DTO
     *
     * @param view 消息视图
     * @return 消息响应 DTO
     */
    public static @NotNull TicketMessageRespond toMessageRespond(@NotNull TicketMessageView view) {
        return TicketMessageRespond.builder()
                .id(view.id())
                .messageNo(view.messageNo())
                .ticketId(view.ticketId())
                .senderType(view.senderType())
                .senderUserId(view.senderUserId())
                .messageType(view.messageType())
                .content(view.content())
                .attachments(view.attachments())
                .clientMessageId(view.clientMessageId())
                .sentAt(view.sentAt())
                .editedAt(view.editedAt())
                .recalledAt(view.recalledAt())
                .build();
    }

    /**
     * 已读位点更新视图转换为 WebSocket 事件数据响应 DTO
     *
     * @param view 已读位点更新视图
     * @return 已读事件数据响应 DTO
     */
    public static @NotNull CsWsTicketReadUpdatedEventDataRespond toReadUpdatedEventDataRespond(@NotNull TicketReadUpdateView view) {
        return CsWsTicketReadUpdatedEventDataRespond.builder()
                .ticketId(view.ticketId())
                .participantId(view.participantId())
                .participantType(view.participantType())
                .participantUserId(view.participantUserId())
                .lastReadMessageId(view.lastReadMessageId())
                .lastReadAt(view.lastReadAt())
                .build();
    }

    /**
     * WebSocket 会话签发视图转换为响应 DTO
     *
     * @param view 会话签发视图
     * @return 会话签发响应 DTO
     */
    public static @NotNull CsWsSessionIssueDataRespond toWsSessionIssueDataRespond(@NotNull TicketWsSessionIssueView view) {
        return CsWsSessionIssueDataRespond.builder()
                .wsToken(view.wsToken())
                .wsUrl(view.wsUrl())
                .issuedAt(view.issuedAt())
                .expiresAt(view.expiresAt())
                .heartbeatIntervalSeconds(view.heartbeatIntervalSeconds())
                .resumeTtlSeconds(view.resumeTtlSeconds())
                .build();
    }
}
