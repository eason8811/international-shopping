package shopping.international.domain.model.entity.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketParticipantRole;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单参与方实体, 对应表 `cs_ticket_participant`
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class TicketParticipant implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 工单 ID
     */
    private Long ticketId;
    /**
     * 参与方类型
     */
    private TicketParticipantType participantType;
    /**
     * 参与方用户 ID, 当类型为 SYSTEM 或 BOT 时可为空
     */
    @Nullable
    private Long participantUserId;
    /**
     * 参与方角色
     */
    private TicketParticipantRole role;
    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
    /**
     * 离开时间, 为空表示仍处于活跃状态
     */
    @Nullable
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
    private LocalDateTime lastReadAt;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 构造工单参与方实体
     *
     * @param id                主键 ID
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     * @param joinedAt          加入时间
     * @param leftAt            离开时间
     * @param lastReadMessageId 最后已读消息 ID
     * @param lastReadAt        最后已读时间
     * @param createdAt         创建时间
     * @param updatedAt         更新时间
     */
    private TicketParticipant(@Nullable Long id,
                              Long ticketId,
                              TicketParticipantType participantType,
                              @Nullable Long participantUserId,
                              TicketParticipantRole role,
                              LocalDateTime joinedAt,
                              @Nullable LocalDateTime leftAt,
                              @Nullable Long lastReadMessageId,
                              @Nullable LocalDateTime lastReadAt,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.participantType = participantType;
        this.participantUserId = participantUserId;
        this.role = role;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.lastReadMessageId = lastReadMessageId;
        this.lastReadAt = lastReadAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新的工单参与方实体
     *
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     * @return 新建的工单参与方实体
     */
    public static TicketParticipant create(Long ticketId,
                                           TicketParticipantType participantType,
                                           @Nullable Long participantUserId,
                                           TicketParticipantRole role) {
        LocalDateTime now = LocalDateTime.now();
        TicketParticipant participant = new TicketParticipant(
                null,
                ticketId,
                participantType,
                participantUserId,
                role,
                now,
                null,
                null,
                null,
                now,
                now
        );
        participant.validate();
        return participant;
    }

    /**
     * 从持久化数据重建工单参与方实体
     *
     * @param id                主键 ID
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     * @param joinedAt          加入时间
     * @param leftAt            离开时间
     * @param lastReadMessageId 最后已读消息 ID
     * @param lastReadAt        最后已读时间
     * @param createdAt         创建时间
     * @param updatedAt         更新时间
     * @return 重建后的工单参与方实体
     */
    public static TicketParticipant reconstitute(@Nullable Long id,
                                                 Long ticketId,
                                                 TicketParticipantType participantType,
                                                 @Nullable Long participantUserId,
                                                 TicketParticipantRole role,
                                                 LocalDateTime joinedAt,
                                                 @Nullable LocalDateTime leftAt,
                                                 @Nullable Long lastReadMessageId,
                                                 @Nullable LocalDateTime lastReadAt,
                                                 LocalDateTime createdAt,
                                                 LocalDateTime updatedAt) {
        TicketParticipant participant = new TicketParticipant(
                id,
                ticketId,
                participantType,
                participantUserId,
                role,
                joinedAt,
                leftAt,
                lastReadMessageId,
                lastReadAt,
                createdAt,
                updatedAt
        );
        participant.validate();
        return participant;
    }

    /**
     * 判断当前参与方是否活跃
     *
     * @return 若 `leftAt` 为空, 返回 true
     */
    public boolean isActive() {
        return leftAt == null;
    }

    /**
     * 修改参与方角色
     *
     * @param newRole 新角色
     */
    public void changeRole(TicketParticipantRole newRole) {
        requireNotNull(newRole, "newRole 不能为空");
        this.role = newRole;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记参与方离开会话
     *
     * @param leftAtTime 离开时间, 为空时默认使用当前时间
     */
    public void leave(@Nullable LocalDateTime leftAtTime) {
        if (leftAt != null)
            throw new ConflictException("参与方已离开会话");
        this.leftAt = leftAtTime == null ? LocalDateTime.now() : leftAtTime;
        this.updatedAt = this.leftAt;
    }

    /**
     * 更新参与方已读位点
     *
     * @param newLastReadMessageId 新的最后已读消息 ID
     * @param readAtTime           已读时间, 为空时默认使用当前时间
     */
    public void markRead(Long newLastReadMessageId, @Nullable LocalDateTime readAtTime) {
        requireNotNull(newLastReadMessageId, "lastReadMessageId 不能为空");
        require(newLastReadMessageId >= 1, "lastReadMessageId 必须大于等于 1");
        if (lastReadMessageId != null)
            require(newLastReadMessageId >= lastReadMessageId, "lastReadMessageId 不允许回退");
        this.lastReadMessageId = newLastReadMessageId;
        this.lastReadAt = readAtTime == null ? LocalDateTime.now() : readAtTime;
        this.updatedAt = this.lastReadAt;
    }

    /**
     * 校验工单参与方实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        requireNotNull(ticketId, "ticketId 不能为空");
        require(ticketId > 0, "ticketId 必须大于 0");
        requireNotNull(participantType, "participantType 不能为空");
        if (participantType.requiresUserId()) {
            requireNotNull(participantUserId, "当前 participantType 必须提供 participantUserId");
            require(participantUserId > 0, "participantUserId 必须大于 0");
        } else if (participantUserId != null)
            require(participantUserId > 0, "participantUserId 必须大于 0");
        requireNotNull(role, "role 不能为空");
        requireNotNull(joinedAt, "joinedAt 不能为空");
        if (lastReadMessageId != null)
            require(lastReadMessageId > 0, "lastReadMessageId 必须大于 0");
        requireNotNull(createdAt, "createdAt 不能为空");
        requireNotNull(updatedAt, "updatedAt 不能为空");
        if (leftAt != null)
            require(!leftAt.isBefore(joinedAt), "leftAt 不可早于 joinedAt");
        if (lastReadAt != null)
            require(!lastReadAt.isBefore(joinedAt), "lastReadAt 不可早于 joinedAt");
    }
}
