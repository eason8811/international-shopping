package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketParticipantRole;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;

import java.time.LocalDateTime;

/**
 * 工单参与方响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketParticipantRespond {
    /**
     * 参与方记录 ID
     */
    @NotNull
    private Long id;
    /**
     * 工单 ID
     */
    @NotNull
    private Long ticketId;
    /**
     * 参与方类型
     */
    @NotNull
    private TicketParticipantType participantType;
    /**
     * 参与方用户 ID
     */
    @Nullable
    private Long participantUserId;
    /**
     * 工单内角色
     */
    @NotNull
    private TicketParticipantRole role;
    /**
     * 加入时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedAt;
    /**
     * 离开时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime leftAt;
    /**
     * 最后已读消息 ID
     */
    @Nullable
    private Long lastReadMessageId;
    /**
     * 最后已读时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReadAt;
}
