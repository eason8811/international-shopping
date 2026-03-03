package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;

import java.time.LocalDateTime;

/**
 * 用户侧工单已读位点更新结果视图值对象
 *
 * @param ticketId           工单 ID
 * @param participantId      参与方 ID
 * @param participantType    参与方类型
 * @param participantUserId  参与方用户 ID
 * @param lastReadMessageId  最后已读消息 ID
 * @param lastReadAt         最后已读时间
 */
public record TicketReadUpdateView(Long ticketId,
                                   Long participantId,
                                   TicketParticipantType participantType,
                                   @Nullable Long participantUserId,
                                   Long lastReadMessageId,
                                   LocalDateTime lastReadAt) {
}

