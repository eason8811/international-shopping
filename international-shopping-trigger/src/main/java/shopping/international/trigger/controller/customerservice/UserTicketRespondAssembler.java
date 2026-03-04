package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.api.resp.customerservice.CsWsSessionIssueDataRespond;
import shopping.international.api.resp.customerservice.CsWsTicketReadUpdatedEventDataRespond;
import shopping.international.api.resp.customerservice.ShipmentSummaryRespond;
import shopping.international.api.resp.customerservice.TicketCreateDataRespond;
import shopping.international.api.resp.customerservice.TicketMessageRespond;
import shopping.international.api.resp.customerservice.TicketStatusLogRespond;
import shopping.international.api.resp.customerservice.UserTicketDetailRespond;
import shopping.international.api.resp.customerservice.UserTicketSummaryRespond;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.types.currency.CurrencyConfig;

/**
 * 用户侧工单响应装配器, 负责 View 和 DTO 之间的转换
 */
public final class UserTicketRespondAssembler {

    /**
     * 私有构造方法, 工具类不允许实例化
     */
    private UserTicketRespondAssembler() {
    }

    /**
     * 用户侧工单摘要视图转换为响应 DTO
     *
     * @param view              摘要视图
     * @param payCurrencyConfig 支付金额币种配置
     * @return 摘要响应 DTO
     */
    public static @NotNull UserTicketSummaryRespond toSummaryRespond(@NotNull UserTicketSummaryView view,
                                                                     @NotNull CurrencyConfig payCurrencyConfig) {
        return UserTicketSummaryRespond.builder()
                .ticketNo(view.ticketNo())
                .issueType(view.issueType())
                .status(view.status())
                .title(view.title())
                .orderId(view.orderId())
                .orderNo(view.orderNo())
                .orderStatus(view.orderStatus())
                .payAmount(payCurrencyConfig.toMajor(view.payAmountMinor()).toPlainString())
                .orderCover(view.orderCover())
                .shipmentId(view.shipmentId())
                .shipmentStatus(view.shipmentStatus())
                .shipmentStatusLogSnapshot(view.shipmentStatusLogSnapshot())
                .assignedToUserId(view.assignedToUserId())
                .lastMessageAt(view.lastMessageAt())
                .createdAt(view.createdAt())
                .updatedAt(view.updatedAt())
                .build();
    }

    /**
     * 用户侧工单详情视图转换为响应 DTO
     *
     * @param view              详情视图
     * @param payCurrencyConfig 支付金额币种配置
     * @return 详情响应 DTO
     */
    public static @NotNull UserTicketDetailRespond toDetailRespond(@NotNull UserTicketDetailView view,
                                                                   @NotNull CurrencyConfig payCurrencyConfig) {
        return UserTicketDetailRespond.builder()
                .ticketNo(view.ticketNo())
                .issueType(view.issueType())
                .status(view.status())
                .title(view.title())
                .orderId(view.orderId())
                .orderNo(view.orderNo())
                .orderStatus(view.orderStatus())
                .payAmount(payCurrencyConfig.toMajor(view.payAmountMinor()).toPlainString())
                .orderCover(view.orderCover())
                .shipmentId(view.shipmentId())
                .shipmentStatus(view.shipmentStatus())
                .shipmentStatusLogSnapshot(view.shipmentStatusLogSnapshot())
                .assignedToUserId(view.assignedToUserId())
                .lastMessageAt(view.lastMessageAt())
                .createdAt(view.createdAt())
                .updatedAt(view.updatedAt())
                .description(view.description())
                .attachments(view.attachments())
                .evidence(view.evidence())
                .tags(view.tags())
                .requestedRefundAmount(view.requestedRefundAmount())
                .currency(view.currency())
                .resolvedAt(view.resolvedAt())
                .closedAt(view.closedAt())
                .slaDueAt(view.slaDueAt())
                .build();
    }

    /**
     * 创建结果视图转换为响应 DTO
     *
     * @param view 创建结果视图
     * @return 创建结果响应 DTO
     */
    public static @NotNull TicketCreateDataRespond toCreateDataRespond(@NotNull UserTicketCreateResult view) {
        return TicketCreateDataRespond.builder()
                .ticketId(view.ticketId())
                .ticketNo(view.ticketNo())
                .status(view.status())
                .createdAt(view.createdAt())
                .build();
    }

    /**
     * 用户侧消息视图转换为响应 DTO
     *
     * @param view 消息视图
     * @return 消息响应 DTO
     */
    public static @NotNull TicketMessageRespond toMessageRespond(@NotNull TicketMessageView view) {
        return TicketCommonRespondAssembler.toMessageRespond(view);
    }

    /**
     * 用户侧状态日志视图转换为响应 DTO
     *
     * @param view 状态日志视图
     * @return 状态日志响应 DTO
     */
    public static @NotNull TicketStatusLogRespond toStatusLogRespond(@NotNull UserTicketStatusLogView view) {
        return TicketStatusLogRespond.builder()
                .id(view.id())
                .ticketId(view.ticketId())
                .fromStatus(view.fromStatus())
                .toStatus(view.toStatus())
                .actorType(view.actorType())
                .actorUserId(view.actorUserId())
                .sourceRef(view.sourceRef())
                .note(view.note())
                .createdAt(view.createdAt())
                .build();
    }

    /**
     * 用户侧工单关联物流视图转换为响应 DTO
     *
     * @param view 物流摘要视图
     * @return 物流摘要响应 DTO
     */
    public static @NotNull ShipmentSummaryRespond toShipmentSummaryRespond(@NotNull UserTicketShipmentSummaryView view) {
        return ShipmentSummaryRespond.builder()
                .id(view.id())
                .shipmentNo(view.shipmentNo())
                .orderId(view.orderId())
                .orderNo(view.orderNo())
                .idempotencyKey(view.idempotencyKey())
                .carrierCode(view.carrierCode())
                .carrierName(view.carrierName())
                .serviceCode(view.serviceCode())
                .trackingNo(view.trackingNo())
                .extExternalId(view.extExternalId())
                .status(view.status())
                .pickupTime(view.pickupTime())
                .deliveredTime(view.deliveredTime())
                .currency(view.currency())
                .createdAt(view.createdAt())
                .updatedAt(view.updatedAt())
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
