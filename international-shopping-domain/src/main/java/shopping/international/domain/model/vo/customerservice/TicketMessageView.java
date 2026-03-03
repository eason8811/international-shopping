package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧工单消息视图值对象, 用于消息列表和消息详情输出
 *
 * @param id               消息 ID
 * @param messageNo        消息编号
 * @param ticketId         工单 ID
 * @param senderType       发送方类型
 * @param senderUserId     发送方用户 ID
 * @param messageType      消息类型
 * @param content          消息内容
 * @param attachments      附件列表
 * @param clientMessageId  客户端消息幂等键
 * @param sentAt           发送时间
 * @param editedAt         编辑时间
 * @param recalledAt       撤回时间
 */
public record TicketMessageView(Long id,
                                String messageNo,
                                Long ticketId,
                                TicketParticipantType senderType,
                                @Nullable Long senderUserId,
                                TicketMessageType messageType,
                                @Nullable String content,
                                List<String> attachments,
                                @Nullable String clientMessageId,
                                LocalDateTime sentAt,
                                @Nullable LocalDateTime editedAt,
                                @Nullable LocalDateTime recalledAt) {

    /**
     * 规范化构造, 避免可选集合字段为 null
     */
    public TicketMessageView {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}

