package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketAssignmentActionType;

import java.time.LocalDateTime;

/**
 * 工单指派日志响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignmentLogRespond {
    /**
     * 日志 ID
     */
    @NotNull
    private Long id;
    /**
     * 工单 ID
     */
    @NotNull
    private Long ticketId;
    /**
     * 原指派用户 ID
     */
    @Nullable
    private Long fromAssigneeUserId;
    /**
     * 新指派用户 ID
     */
    @Nullable
    private Long toAssigneeUserId;
    /**
     * 指派动作类型
     */
    @NotNull
    private TicketAssignmentActionType actionType;
    /**
     * 操作者类型
     */
    @NotNull
    private TicketActorType actorType;
    /**
     * 操作者用户 ID
     */
    @Nullable
    private Long actorUserId;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;
    /**
     * 备注信息
     */
    @Nullable
    private String note;
    /**
     * 创建时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
