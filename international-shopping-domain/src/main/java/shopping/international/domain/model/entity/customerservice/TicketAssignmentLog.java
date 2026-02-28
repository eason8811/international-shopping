package shopping.international.domain.model.entity.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketAssignmentActionType;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单指派日志实体, 对应表 `cs_ticket_assignment_log`
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class TicketAssignmentLog implements Verifiable {

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
    private TicketAssignmentActionType actionType;
    /**
     * 操作者类型
     */
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
     * 备注
     */
    @Nullable
    private String note;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 构造工单指派日志实体
     *
     * @param id                 主键 ID
     * @param ticketId           工单 ID
     * @param fromAssigneeUserId 原指派用户 ID
     * @param toAssigneeUserId   新指派用户 ID
     * @param actionType         指派动作类型
     * @param actorType          操作者类型
     * @param actorUserId        操作者用户 ID
     * @param sourceRef          来源引用 ID
     * @param note               备注
     * @param createdAt          创建时间
     */
    private TicketAssignmentLog(@Nullable Long id,
                                Long ticketId,
                                @Nullable Long fromAssigneeUserId,
                                @Nullable Long toAssigneeUserId,
                                TicketAssignmentActionType actionType,
                                TicketActorType actorType,
                                @Nullable Long actorUserId,
                                @Nullable String sourceRef,
                                @Nullable String note,
                                LocalDateTime createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.fromAssigneeUserId = fromAssigneeUserId;
        this.toAssigneeUserId = toAssigneeUserId;
        this.actionType = actionType;
        this.actorType = actorType;
        this.actorUserId = actorUserId;
        this.sourceRef = sourceRef;
        this.note = note;
        this.createdAt = createdAt;
    }

    /**
     * 创建工单指派日志实体
     *
     * @param ticketId           工单 ID
     * @param fromAssigneeUserId 原指派用户 ID
     * @param toAssigneeUserId   新指派用户 ID
     * @param actionType         指派动作类型
     * @param actorType          操作者类型
     * @param actorUserId        操作者用户 ID
     * @param sourceRef          来源引用 ID
     * @param note               备注
     * @return 新建的工单指派日志实体
     */
    public static TicketAssignmentLog create(Long ticketId,
                                             @Nullable Long fromAssigneeUserId,
                                             @Nullable Long toAssigneeUserId,
                                             TicketAssignmentActionType actionType,
                                             TicketActorType actorType,
                                             @Nullable Long actorUserId,
                                             @Nullable String sourceRef,
                                             @Nullable String note) {
        TicketAssignmentLog log = new TicketAssignmentLog(
                null,
                ticketId,
                fromAssigneeUserId,
                toAssigneeUserId,
                actionType,
                actorType,
                actorUserId,
                normalizeNullableField(sourceRef, "sourceRef 不能为空", value -> value.length() <= 128, "sourceRef 长度不能超过 128"),
                normalizeNullableField(note, "note 不能为空", value -> value.length() <= 255, "note 长度不能超过 255"),
                LocalDateTime.now()
        );
        log.validate();
        return log;
    }

    /**
     * 从持久化数据重建工单指派日志实体
     *
     * @param id                 主键 ID
     * @param ticketId           工单 ID
     * @param fromAssigneeUserId 原指派用户 ID
     * @param toAssigneeUserId   新指派用户 ID
     * @param actionType         指派动作类型
     * @param actorType          操作者类型
     * @param actorUserId        操作者用户 ID
     * @param sourceRef          来源引用 ID
     * @param note               备注
     * @param createdAt          创建时间
     * @return 重建后的工单指派日志实体
     */
    public static TicketAssignmentLog reconstitute(@Nullable Long id,
                                                   Long ticketId,
                                                   @Nullable Long fromAssigneeUserId,
                                                   @Nullable Long toAssigneeUserId,
                                                   TicketAssignmentActionType actionType,
                                                   TicketActorType actorType,
                                                   @Nullable Long actorUserId,
                                                   @Nullable String sourceRef,
                                                   @Nullable String note,
                                                   LocalDateTime createdAt) {
        TicketAssignmentLog log = new TicketAssignmentLog(
                id,
                ticketId,
                fromAssigneeUserId,
                toAssigneeUserId,
                actionType,
                actorType,
                actorUserId,
                normalizeNullableField(sourceRef, "sourceRef 不能为空", value -> value.length() <= 128, "sourceRef 长度不能超过 128"),
                normalizeNullableField(note, "note 不能为空", value -> value.length() <= 255, "note 长度不能超过 255"),
                createdAt
        );
        log.validate();
        return log;
    }

    /**
     * 校验工单指派日志实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        requireNotNull(ticketId, "ticketId 不能为空");
        require(ticketId > 0, "ticketId 必须大于 0");
        if (fromAssigneeUserId != null)
            require(fromAssigneeUserId > 0, "fromAssigneeUserId 必须大于 0");
        if (toAssigneeUserId != null)
            require(toAssigneeUserId > 0, "toAssigneeUserId 必须大于 0");
        requireNotNull(actionType, "actionType 不能为空");
        if (actionType.requiresAssignee())
            requireNotNull(toAssigneeUserId, "当前 actionType 需要提供 toAssigneeUserId");
        requireNotNull(actorType, "actorType 不能为空");
        if (actorUserId != null)
            require(actorUserId > 0, "actorUserId 必须大于 0");
        if (sourceRef != null)
            require(sourceRef.length() <= 128, "sourceRef 长度不能超过 128");
        if (note != null)
            require(note.length() <= 255, "note 长度不能超过 255");
        requireNotNull(createdAt, "createdAt 不能为空");
    }
}
