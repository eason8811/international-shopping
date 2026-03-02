package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;

import java.time.LocalDateTime;

/**
 * 用户侧工单摘要视图值对象, 用于列表查询结果传输
 *
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
 */
public record UserTicketSummaryView(String ticketNo,
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
                                    LocalDateTime updatedAt) {

    /**
     * 规范化构造, 兜底处理可选字符串字段
     */
    public UserTicketSummaryView {
        shipmentStatusLogSnapshot = shipmentStatusLogSnapshot == null ? "" : shipmentStatusLogSnapshot;
    }
}
