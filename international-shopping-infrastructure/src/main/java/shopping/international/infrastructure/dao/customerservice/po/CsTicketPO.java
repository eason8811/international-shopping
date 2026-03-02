package shopping.international.infrastructure.dao.customerservice.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象, cs_ticket 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("cs_ticket")
public class CsTicketPO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 工单编号
     */
    @TableField("ticket_no")
    private String ticketNo;
    /**
     * 发起用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 关联订单 ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 关联订单明细 ID
     */
    @TableField("order_item_id")
    private Long orderItemId;
    /**
     * 关联物流单 ID
     */
    @TableField("shipment_id")
    private Long shipmentId;
    /**
     * 问题类型
     */
    @TableField("issue_type")
    private String issueType;
    /**
     * 工单状态
     */
    @TableField("status")
    private String status;
    /**
     * 工单优先级
     */
    @TableField("priority")
    private String priority;
    /**
     * 工单来源渠道
     */
    @TableField("channel")
    private String channel;
    /**
     * 工单标题
     */
    @TableField("title")
    private String title;
    /**
     * 工单描述
     */
    @TableField("description")
    private String description;
    /**
     * 附件 JSON
     */
    @TableField("attachments")
    private String attachments;
    /**
     * 证据 JSON
     */
    @TableField("evidence")
    private String evidence;
    /**
     * 标签 JSON
     */
    @TableField("tags")
    private String tags;
    /**
     * 申请退款金额, 最小货币单位
     */
    @TableField("requested_refund_amount")
    private Long requestedRefundAmount;
    /**
     * 币种
     */
    @TableField("currency")
    private String currency;
    /**
     * 理赔外部编号
     */
    @TableField("claim_external_id")
    private String claimExternalId;
    /**
     * 指派坐席用户 ID
     */
    @TableField("assigned_to_user_id")
    private Long assignedToUserId;
    /**
     * 指派时间
     */
    @TableField("assigned_at")
    private LocalDateTime assignedAt;
    /**
     * 最近消息时间
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;
    /**
     * SLA 到期时间
     */
    @TableField("sla_due_at")
    private LocalDateTime slaDueAt;
    /**
     * 解决时间
     */
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;
    /**
     * 关闭时间
     */
    @TableField("closed_at")
    private LocalDateTime closedAt;
    /**
     * 当前状态进入时间
     */
    @TableField("status_changed_at")
    private LocalDateTime statusChangedAt;
    /**
     * 来源引用 ID
     */
    @TableField("source_ref")
    private String sourceRef;
    /**
     * 扩展 JSON
     */
    @TableField("extra")
    private String extra;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
