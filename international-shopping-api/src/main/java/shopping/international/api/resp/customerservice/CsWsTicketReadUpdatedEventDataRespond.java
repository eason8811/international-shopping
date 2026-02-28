package shopping.international.api.resp.customerservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;

import java.time.LocalDateTime;

/**
 * WebSocket 工单已读位点变更事件数据响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWsTicketReadUpdatedEventDataRespond {
    /**
     * 工单 ID
     */
    @NotNull
    private Long ticketId;
    /**
     * 参与方 ID
     */
    @NotNull
    private Long participantId;
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
     * 最后已读消息 ID
     */
    @NotNull
    private Long lastReadMessageId;
    /**
     * 最后已读时间
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReadAt;
}
