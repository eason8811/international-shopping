package shopping.international.infrastructure.dao.customerservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单查询投影对象, 同时覆盖用户侧和管理侧摘要与详情查询字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsTicketQueryPO {

    /**
     * 工单 ID
     */
    private Long ticketId;
    /**
     * 工单编号
     */
    private String ticketNo;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 问题类型
     */
    private String issueType;
    /**
     * 工单状态
     */
    private String status;
    /**
     * 工单优先级
     */
    private String priority;
    /**
     * 工单来源渠道
     */
    private String channel;
    /**
     * 工单标题
     */
    private String title;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单明细 ID
     */
    private Long orderItemId;
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
     * 指派时间
     */
    private LocalDateTime assignedAt;
    /**
     * 最近消息时间
     */
    private LocalDateTime lastMessageAt;
    /**
     * SLA 到期时间
     */
    private LocalDateTime slaDueAt;
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
     * 理赔外部编号
     */
    private String claimExternalId;
    /**
     * 来源引用 ID
     */
    private String sourceRef;
    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;
    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;
}
