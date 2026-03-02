package shopping.international.domain.model.vo.customerservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧工单分页查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketPageCriteria implements Verifiable {

    /**
     * 工单编号
     */
    @Nullable
    private String ticketNo;
    /**
     * 用户 ID
     */
    @Nullable
    private Long userId;
    /**
     * 订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 物流单 ID
     */
    @Nullable
    private Long shipmentId;
    /**
     * 问题类型
     */
    @Nullable
    private TicketIssueType issueType;
    /**
     * 工单状态
     */
    @Nullable
    private TicketStatus status;
    /**
     * 工单优先级
     */
    @Nullable
    private TicketPriority priority;
    /**
     * 指派坐席用户 ID
     */
    @Nullable
    private Long assignedToUserId;
    /**
     * 理赔外部编号
     */
    @Nullable
    private String claimExternalId;
    /**
     * SLA 到期时间起始
     */
    @Nullable
    private LocalDateTime slaDueFrom;
    /**
     * SLA 到期时间结束
     */
    @Nullable
    private LocalDateTime slaDueTo;
    /**
     * 创建时间起始
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 创建时间结束
     */
    @Nullable
    private LocalDateTime createdTo;

    /**
     * 校验管理侧工单分页查询条件
     */
    @Override
    public void validate() {
        if (userId != null)
            require(userId >= 1, "userId 必须大于等于 1");
        if (orderId != null)
            require(orderId >= 1, "orderId 必须大于等于 1");
        if (shipmentId != null)
            require(shipmentId >= 1, "shipmentId 必须大于等于 1");
        if (assignedToUserId != null)
            require(assignedToUserId >= 1, "assignedToUserId 必须大于等于 1");

        if (slaDueFrom != null && slaDueTo != null)
            require(!slaDueFrom.isAfter(slaDueTo), "slaDueFrom 不能晚于 slaDueTo");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");
    }
}
