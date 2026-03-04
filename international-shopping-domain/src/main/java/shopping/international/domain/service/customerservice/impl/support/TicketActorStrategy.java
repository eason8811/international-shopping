package shopping.international.domain.service.customerservice.impl.support;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.types.exceptions.ConflictException;

import java.util.Objects;

/**
 * 工单消息操作者策略, 用于区分 USER 和 AGENT 的消息权限语义
 */
public enum TicketActorStrategy {

    /**
     * 用户侧操作者策略
     */
    USER(
            TicketParticipantType.USER,
            "当前消息不允许用户执行该操作",
            null
    ),
    /**
     * 管理侧操作者策略
     */
    AGENT(
            TicketParticipantType.AGENT,
            "当前消息不允许坐席执行该操作",
            "agent"
    );

    /**
     * 消息发送者类型
     */
    private final TicketParticipantType participantType;
    /**
     * 发送者类型不匹配时的报错文案
     */
    private final String invalidSenderTypeMessage;
    /**
     * WebSocket 角色声明值, 用户侧为 null
     */
    private final String wsRoleClaim;

    /**
     * 构造操作者策略
     *
     * @param participantType         消息发送者类型
     * @param invalidSenderTypeMessage 发送者类型不匹配文案
     * @param wsRoleClaim             WebSocket 角色声明值
     */
    TicketActorStrategy(@NotNull TicketParticipantType participantType,
                        @NotNull String invalidSenderTypeMessage,
                        @Nullable String wsRoleClaim) {
        this.participantType = participantType;
        this.invalidSenderTypeMessage = invalidSenderTypeMessage;
        this.wsRoleClaim = wsRoleClaim;
    }

    /**
     * 获取消息发送者类型
     *
     * @return 发送者类型
     */
    public @NotNull TicketParticipantType participantType() {
        return participantType;
    }

    /**
     * 获取 WebSocket 角色声明值
     *
     * @return 角色声明值, 用户侧返回 null
     */
    public @Nullable String wsRoleClaim() {
        return wsRoleClaim;
    }

    /**
     * 校验消息操作者是否有权执行编辑或撤回操作
     *
     * @param actorUserId 操作者用户 ID
     * @param message     消息实体
     */
    public void validateMessageOperator(@NotNull Long actorUserId,
                                        @NotNull TicketMessage message) {
        if (message.getSenderType() != participantType)
            throw new ConflictException(invalidSenderTypeMessage);
        if (!Objects.equals(message.getSenderUserId(), actorUserId))
            throw new ConflictException("无权操作该消息");
    }
}
