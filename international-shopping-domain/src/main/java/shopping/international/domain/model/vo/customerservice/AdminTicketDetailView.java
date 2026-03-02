package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketChannel;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理侧工单详情视图值对象, 用于详情查询和写操作后的回包
 *
 * @param ticketId              工单 ID
 * @param ticketNo              工单编号
 * @param userId                用户 ID
 * @param issueType             问题类型
 * @param status                工单状态
 * @param priority              工单优先级
 * @param channel               工单来源渠道
 * @param title                 工单标题
 * @param orderId               订单 ID
 * @param orderItemId           订单明细 ID
 * @param orderNo               订单号
 * @param orderStatus           订单状态
 * @param payAmountMinor        订单支付金额, 最小货币单位
 * @param payCurrency           订单支付币种
 * @param orderCover            订单封面图
 * @param shipmentId            物流单 ID
 * @param shipmentStatus        物流状态
 * @param shipmentStatusLogSnapshot 物流状态快照说明
 * @param assignedToUserId      指派坐席用户 ID
 * @param assignedAt            指派时间
 * @param lastMessageAt         最近消息时间
 * @param slaDueAt              SLA 到期时间
 * @param createdAt             创建时间
 * @param updatedAt             更新时间
 * @param description           工单描述
 * @param attachments           附件链接列表
 * @param evidence              证据链接列表
 * @param tags                  标签列表
 * @param requestedRefundAmount 申请退款金额, 最小货币单位
 * @param currency              币种
 * @param claimExternalId       理赔外部编号
 * @param sourceRef             来源引用 ID
 * @param resolvedAt            解决时间
 * @param closedAt              关闭时间
 */
public record AdminTicketDetailView(Long ticketId,
                                    String ticketNo,
                                    Long userId,
                                    TicketIssueType issueType,
                                    TicketStatus status,
                                    TicketPriority priority,
                                    TicketChannel channel,
                                    String title,
                                    @Nullable Long orderId,
                                    @Nullable Long orderItemId,
                                    String orderNo,
                                    OrderStatus orderStatus,
                                    long payAmountMinor,
                                    String payCurrency,
                                    String orderCover,
                                    @Nullable Long shipmentId,
                                    @Nullable ShipmentStatus shipmentStatus,
                                    String shipmentStatusLogSnapshot,
                                    @Nullable Long assignedToUserId,
                                    @Nullable LocalDateTime assignedAt,
                                    @Nullable LocalDateTime lastMessageAt,
                                    @Nullable LocalDateTime slaDueAt,
                                    LocalDateTime createdAt,
                                    LocalDateTime updatedAt,
                                    @Nullable String description,
                                    List<String> attachments,
                                    List<String> evidence,
                                    List<String> tags,
                                    @Nullable Long requestedRefundAmount,
                                    @Nullable String currency,
                                    @Nullable String claimExternalId,
                                    @Nullable String sourceRef,
                                    @Nullable LocalDateTime resolvedAt,
                                    @Nullable LocalDateTime closedAt) {

    /**
     * 规范化构造, 兜底处理集合字段
     */
    public AdminTicketDetailView {
        shipmentStatusLogSnapshot = shipmentStatusLogSnapshot == null ? "" : shipmentStatusLogSnapshot;
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
