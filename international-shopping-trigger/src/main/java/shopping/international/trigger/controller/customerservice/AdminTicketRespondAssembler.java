package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.api.resp.customerservice.AdminTicketDetailRespond;
import shopping.international.api.resp.customerservice.AdminTicketSummaryRespond;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;

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
     * @param view 摘要视图
     * @return 摘要响应 DTO
     */
    public static @NotNull AdminTicketSummaryRespond toSummaryRespond(@NotNull AdminTicketSummaryView view) {
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
                .shipmentId(view.shipmentId())
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
     * @param view 详情视图
     * @return 详情响应 DTO
     */
    public static @NotNull AdminTicketDetailRespond toDetailRespond(@NotNull AdminTicketDetailView view) {
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
                .shipmentId(view.shipmentId())
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
}
