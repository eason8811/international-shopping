package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧工单详情视图值对象, 用于详情查询与关闭后回包
 *
 * @param ticketId                   工单 ID
 * @param ticketNo                   工单编号
 * @param issueType                  问题类型
 * @param status                     工单状态
 * @param title                      工单标题
 * @param orderId                    订单 ID
 * @param orderNo                    订单号
 * @param orderStatus                订单状态
 * @param payAmountMinor             订单支付金额, 最小货币单位
 * @param payCurrency                订单支付币种
 * @param orderCover                 订单封面图
 * @param shipmentId                 物流单 ID
 * @param shipmentStatus             物流状态
 * @param shipmentStatusLogSnapshot  物流状态快照说明
 * @param assignedToUserId           指派坐席用户 ID
 * @param lastMessageAt              最近消息时间
 * @param createdAt                  创建时间
 * @param updatedAt                  更新时间
 * @param description                工单描述
 * @param attachments                附件链接列表
 * @param evidence                   证据链接列表
 * @param tags                       标签列表
 * @param requestedRefundAmount      申请退款金额, 最小货币单位
 * @param currency                   工单币种
 * @param resolvedAt                 解决时间
 * @param closedAt                   关闭时间
 * @param slaDueAt                   SLA 到期时间
 */
public record UserTicketDetailView(Long ticketId,
                                   String ticketNo,
                                   TicketIssueType issueType,
                                   TicketStatus status,
                                   String title,
                                   Long orderId,
                                   String orderNo,
                                   OrderStatus orderStatus,
                                   long payAmountMinor,
                                   String payCurrency,
                                   String orderCover,
                                   @Nullable Long shipmentId,
                                   @Nullable ShipmentStatus shipmentStatus,
                                   String shipmentStatusLogSnapshot,
                                   @Nullable Long assignedToUserId,
                                   @Nullable LocalDateTime lastMessageAt,
                                   LocalDateTime createdAt,
                                   LocalDateTime updatedAt,
                                   @Nullable String description,
                                   List<String> attachments,
                                   List<String> evidence,
                                   List<String> tags,
                                   @Nullable Long requestedRefundAmount,
                                   @Nullable String currency,
                                   @Nullable LocalDateTime resolvedAt,
                                   @Nullable LocalDateTime closedAt,
                                   @Nullable LocalDateTime slaDueAt) {

    /**
     * 规范化构造, 兜底处理集合和可选字符串字段
     */
    public UserTicketDetailView {
        shipmentStatusLogSnapshot = shipmentStatusLogSnapshot == null ? "" : shipmentStatusLogSnapshot;
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
