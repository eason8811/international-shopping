package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
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
 * 管理侧工单详情响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketDetailRespond {
    /**
     * 工单 ID
     */
    @NotNull
    private Long ticketId;
    /**
     * 工单编号
     */
    @NotNull
    private String ticketNo;
    /**
     * 用户 ID
     */
    @NotNull
    private Long userId;
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
     * 工单优先级
     */
    @NotNull
    private TicketPriority priority;
    /**
     * 工单来源渠道
     */
    @NotNull
    private TicketChannel channel;
    /**
     * 工单标题
     */
    @NotNull
    private String title;
    /**
     * 订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 订单明细 ID
     */
    @Nullable
    private Long orderItemId;
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
     * 指派时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignedAt;
    /**
     * 最近消息时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageAt;
    /**
     * SLA 到期时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime slaDueAt;
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
    /**
     * 工单描述
     */
    @Nullable
    private String description;
    /**
     * 附件链接列表
     */
    @Nullable
    private List<String> attachments;
    /**
     * 证据链接列表
     */
    @Nullable
    private List<String> evidence;
    /**
     * 标签列表
     */
    @Nullable
    private List<String> tags;
    /**
     * 申请退款金额（分）
     */
    @Nullable
    private Long requestedRefundAmount;
    /**
     * 币种代码
     */
    @Nullable
    private String currency;
    /**
     * 理赔外部编号
     */
    @Nullable
    private String claimExternalId;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;
    /**
     * 解决时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
    /**
     * 关闭时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;
}
