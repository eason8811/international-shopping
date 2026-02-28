package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;

import java.time.LocalDateTime;

/**
 * 用户侧工单摘要响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTicketSummaryRespond {
    /**
     * 工单编号
     */
    @NotNull
    private String ticketNo;
    /**
     * 问题类型
     */
    @NotNull
    private TicketIssueType issueType;
    /**
     * 工单状态
     */
    @NotNull
    private TicketStatus status;
    /**
     * 工单标题
     */
    @NotNull
    private String title;
    /**
     * 订单 ID
     */
    @NotNull
    private Long orderId;
    /**
     * 订单号
     */
    @NotNull
    private String orderNo;
    /**
     * 订单状态
     */
    @NotNull
    private OrderStatus orderStatus;
    /**
     * 订单支付金额
     */
    @NotNull
    private String payAmount;
    /**
     * 订单封面图
     */
    @NotNull
    private String orderCover;
    /**
     * 物流单 ID
     */
    @Nullable
    private Long shipmentId;
    /**
     * 物流状态
     */
    @Nullable
    private ShipmentStatus shipmentStatus;
    /**
     * 物流状态快照说明
     */
    @NotNull
    private String shipmentStatusLogSnapshot;
    /**
     * 指派坐席用户 ID
     */
    @Nullable
    private Long assignedToUserId;
    /**
     * 最近消息时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageAt;
    /**
     * 创建时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
