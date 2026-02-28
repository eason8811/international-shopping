package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 管理侧工单摘要响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketSummaryRespond {
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
    private String issueType;
    /**
     * 工单状态
     */
    @NotNull
    private String status;
    /**
     * 工单优先级
     */
    @NotNull
    private String priority;
    /**
     * 工单来源渠道
     */
    @NotNull
    private String channel;
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
     * 物流单 ID
     */
    @Nullable
    private Long shipmentId;
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
}
