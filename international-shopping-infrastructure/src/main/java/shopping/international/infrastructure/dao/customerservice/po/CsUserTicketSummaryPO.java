package shopping.international.infrastructure.dao.customerservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户侧工单摘要投影对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsUserTicketSummaryPO {

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
}
