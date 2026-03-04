package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.api.resp.customerservice.AdminTicketDetailRespond;
import shopping.international.api.resp.customerservice.AdminTicketSummaryRespond;
import shopping.international.api.resp.customerservice.CsWsSessionIssueDataRespond;
import shopping.international.api.resp.customerservice.CsWsTicketReadUpdatedEventDataRespond;
import shopping.international.api.resp.customerservice.TicketAssignmentLogRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.api.resp.customerservice.TicketParticipantRespond;
import shopping.international.api.resp.customerservice.TicketStatusLogRespond;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.NotFoundException;

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
        return TicketCommonRespondAssembler.toMessageRespond(view);
    }

    /**
     * 管理侧工单参与方实体转换为响应 DTO
     *
     * @param participant 参与方实体
     * @return 参与方响应 DTO
     */
    public static @NotNull TicketParticipantRespond toParticipantRespond(@NotNull TicketParticipant participant) {
        if (participant.getId() == null)
            throw new NotFoundException("参与方 ID 缺失");
        return TicketParticipantRespond.builder()
                .id(participant.getId())
                .ticketId(participant.getTicketId())
                .participantType(participant.getParticipantType())
                .participantUserId(participant.getParticipantUserId())
                .role(participant.getRole())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .lastReadMessageId(participant.getLastReadMessageId())
                .lastReadAt(participant.getLastReadAt())
                .build();
    }

    /**
     * 管理侧工单状态日志实体转换为响应 DTO
     *
     * @param statusLog 状态日志实体
     * @return 状态日志响应 DTO
     */
    public static @NotNull TicketStatusLogRespond toStatusLogRespond(@NotNull TicketStatusLog statusLog) {
        if (statusLog.getId() == null)
            throw new NotFoundException("工单状态变更日志 ID 缺失");
        return TicketStatusLogRespond.builder()
                .id(statusLog.getId())
                .ticketId(statusLog.getTicketId())
                .fromStatus(statusLog.getFromStatus())
                .toStatus(statusLog.getToStatus())
                .actorType(statusLog.getActorType())
                .actorUserId(statusLog.getActorUserId())
                .sourceRef(statusLog.getSourceRef())
                .note(statusLog.getNote())
                .createdAt(statusLog.getCreatedAt())
                .build();
    }

    /**
     * 管理侧工单指派日志实体转换为响应 DTO
     *
     * @param assignmentLog 指派日志实体
     * @return 指派日志响应 DTO
     */
    public static @NotNull TicketAssignmentLogRespond toAssignmentLogRespond(@NotNull TicketAssignmentLog assignmentLog) {
        if (assignmentLog.getId() == null)
            throw new NotFoundException("指派日志 ID 缺失");
        return TicketAssignmentLogRespond.builder()
                .id(assignmentLog.getId())
                .ticketId(assignmentLog.getTicketId())
                .fromAssigneeUserId(assignmentLog.getFromAssigneeUserId())
                .toAssigneeUserId(assignmentLog.getToAssigneeUserId())
                .actionType(assignmentLog.getActionType())
                .actorType(assignmentLog.getActorType())
                .actorUserId(assignmentLog.getActorUserId())
                .sourceRef(assignmentLog.getSourceRef())
                .note(assignmentLog.getNote())
                .createdAt(assignmentLog.getCreatedAt())
                .build();
    }

    /**
     * 已读位点更新视图转换为 WebSocket 事件数据响应 DTO
     *
     * @param view 已读位点更新视图
     * @return 已读事件数据响应 DTO
     */
    public static @NotNull CsWsTicketReadUpdatedEventDataRespond toReadUpdatedEventDataRespond(@NotNull TicketReadUpdateView view) {
        return TicketCommonRespondAssembler.toReadUpdatedEventDataRespond(view);
    }

    /**
     * WebSocket 会话签发视图转换为响应 DTO
     *
     * @param view 会话签发视图
     * @return 会话签发响应 DTO
     */
    public static @NotNull CsWsSessionIssueDataRespond toWsSessionIssueDataRespond(@NotNull TicketWsSessionIssueView view) {
        return TicketCommonRespondAssembler.toWsSessionIssueDataRespond(view);
    }
}
