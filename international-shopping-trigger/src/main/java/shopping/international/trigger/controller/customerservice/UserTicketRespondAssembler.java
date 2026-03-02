package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.api.resp.customerservice.TicketCreateDataRespond;
import shopping.international.api.resp.customerservice.UserTicketDetailRespond;
import shopping.international.api.resp.customerservice.UserTicketSummaryRespond;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;
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
}
