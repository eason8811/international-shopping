package shopping.international.infrastructure.dao.customerservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户侧工单详情投影对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsUserTicketDetailPO {

    /**
     * 工单 ID
     */
    private Long ticketId;
    /**
     * 工单编号
     */
    private String ticketNo;
    /**
     * 问题类型
     */
    private String issueType;
    /**
     * 工单状态
     */
    private String status;
    /**
     * 工单标题
     */
    private String title;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 订单状态
     */
    private String orderStatus;
    /**
     * 支付金额, 最小货币单位
     */
    private Long payAmount;
    /**
     * 支付币种
     */
    private String payCurrency;
    /**
     * 订单封面图
     */
    private String orderCover;
    /**
     * 物流单 ID
     */
    private Long shipmentId;
    /**
     * 物流状态
     */
    private String shipmentStatus;
    /**
     * 物流状态快照
     */
    private String shipmentStatusLogSnapshot;
    /**
     * 指派坐席用户 ID
     */
    private Long assignedToUserId;
    /**
     * 最近消息时间
     */
    private LocalDateTime lastMessageAt;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 工单描述
     */
    private String description;
    /**
     * 附件 JSON
     */
    private String attachments;
    /**
     * 证据 JSON
     */
    private String evidence;
    /**
     * 标签 JSON
     */
    private String tags;
    /**
     * 申请退款金额, 最小货币单位
     */
    private Long requestedRefundAmount;
    /**
     * 工单币种
     */
    private String currency;
    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;
    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;
    /**
     * SLA 到期时间
     */
    private LocalDateTime slaDueAt;
}
