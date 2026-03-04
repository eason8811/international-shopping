package shopping.international.domain.service.customerservice.impl.support;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.vo.customerservice.TicketReadUpdateView;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNull;

/**
 * 工单已读位点视图装配器, 统一将参与方实体映射为已读更新视图
 */
public final class TicketReadUpdateViewMapper {

    /**
     * 私有构造方法, 工具类不允许实例化
     */
    private TicketReadUpdateViewMapper() {
    }

    /**
     * 将参与方实体映射为已读位点更新视图
     *
     * @param ticketId    工单 ID
     * @param participant 参与方实体
     * @return 已读位点更新视图
     */
    public static @NotNull TicketReadUpdateView toReadUpdateView(@NotNull Long ticketId,
                                                                 @NotNull TicketParticipant participant) {
        return new TicketReadUpdateView(
                ticketId,
                normalizeNotNull(participant.getId(), "participantId 不能为空"),
                participant.getParticipantType(),
                participant.getParticipantUserId(),
                normalizeNotNull(participant.getLastReadMessageId(), "lastReadMessageId 不能为空"),
                normalizeNotNull(participant.getLastReadAt(), "lastReadAt 不能为空")
        );
    }
}
