package shopping.international.infrastructure.dao.customerservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理侧工单摘要投影对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsAdminTicketSummaryPO {

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
     * 物流单 ID
     */
    private Long shipmentId;
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
}
