package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.api.resp.customerservice.AdminTicketDetailRespond;
import shopping.international.api.resp.customerservice.AdminTicketSummaryRespond;
import shopping.international.api.resp.customerservice.CsWsSessionIssueDataRespond;
import shopping.international.api.resp.customerservice.CsWsTicketReadUpdatedEventDataRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.types.currency.CurrencyConfig;

/**
 * 管理侧工单响应装配器, 负责 View 和 DTO 之间的转换
 */
public final class AdminTicketRespondAssembler {

    /**
     * 私有构造方法, 工具类不允许实例化
     */
    private AdminTicketRespondAssembler() {
    }

    /**
     * 管理侧工单摘要视图转换为响应 DTO
     *
     * @param view              摘要视图
     * @param payCurrencyConfig 支付金额币种配置
     * @return 摘要响应 DTO
     */
    public static @NotNull AdminTicketSummaryRespond toSummaryRespond(@NotNull AdminTicketSummaryView view,
                                                                      @NotNull CurrencyConfig payCurrencyConfig) {
        return AdminTicketSummaryRespond.builder()
                .ticketId(view.ticketId())
                .ticketNo(view.ticketNo())
                .userId(view.userId())
                .issueType(view.issueType())
                .status(view.status())
                .priority(view.priority())
                .channel(view.channel())
                .title(view.title())
                .orderId(view.orderId())
                .orderItemId(view.orderItemId())
                .orderNo(view.orderNo())
                .orderStatus(view.orderStatus())
                .payAmount(payCurrencyConfig.toMajor(view.payAmountMinor()).toPlainString())
                .orderCover(view.orderCover())
                .shipmentId(view.shipmentId())
                .shipmentStatus(view.shipmentStatus())
                .shipmentStatusLogSnapshot(view.shipmentStatusLogSnapshot())
                .assignedToUserId(view.assignedToUserId())
                .assignedAt(view.assignedAt())
                .lastMessageAt(view.lastMessageAt())
                .slaDueAt(view.slaDueAt())
                .createdAt(view.createdAt())
                .updatedAt(view.updatedAt())
                .build();
    }

    /**
     * 管理侧工单详情视图转换为响应 DTO
     *
     * @param view              详情视图
     * @param payCurrencyConfig 支付金额币种配置
     * @return 详情响应 DTO
     */
    public static @NotNull AdminTicketDetailRespond toDetailRespond(@NotNull AdminTicketDetailView view,
                                                                    @NotNull CurrencyConfig payCurrencyConfig) {
        return AdminTicketDetailRespond.builder()
                .ticketId(view.ticketId())
                .ticketNo(view.ticketNo())
                .userId(view.userId())
                .issueType(view.issueType())
                .status(view.status())
                .priority(view.priority())
                .channel(view.channel())
                .title(view.title())
                .orderId(view.orderId())
                .orderItemId(view.orderItemId())
                .orderNo(view.orderNo())
                .orderStatus(view.orderStatus())
                .payAmount(payCurrencyConfig.toMajor(view.payAmountMinor()).toPlainString())
                .orderCover(view.orderCover())
                .shipmentId(view.shipmentId())
                .shipmentStatus(view.shipmentStatus())
                .shipmentStatusLogSnapshot(view.shipmentStatusLogSnapshot())
                .assignedToUserId(view.assignedToUserId())
                .assignedAt(view.assignedAt())
                .lastMessageAt(view.lastMessageAt())
                .slaDueAt(view.slaDueAt())
                .createdAt(view.createdAt())
                .updatedAt(view.updatedAt())
                .description(view.description())
                .attachments(view.attachments())
                .evidence(view.evidence())
                .tags(view.tags())
                .requestedRefundAmount(view.requestedRefundAmount())
                .currency(view.currency())
                .claimExternalId(view.claimExternalId())
                .sourceRef(view.sourceRef())
                .resolvedAt(view.resolvedAt())
                .closedAt(view.closedAt())
                .build();
    }

    /**
     * 管理侧消息视图转换为响应 DTO
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
