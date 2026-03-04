package shopping.international.domain.service.customerservice.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;
import shopping.international.domain.adapter.repository.customerservice.IAdminTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketAssignmentActionType;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.domain.model.enums.customerservice.TicketParticipantRole;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.domain.service.customerservice.AbstractTicketIdempotentActionService;
import shopping.international.domain.service.customerservice.impl.support.TicketActorStrategy;
import shopping.international.domain.service.customerservice.impl.support.TicketReadUpdateViewMapper;
import shopping.international.domain.service.customerservice.impl.support.TicketWsTokenIssuer;
import shopping.international.domain.service.customerservice.IAdminTicketService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNull;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧工单领域服务实现
 */
@Service
public class AdminTicketService extends AbstractTicketIdempotentActionService implements IAdminTicketService {

    /**
     * 管理侧工单仓储
     */
    private final IAdminTicketRepository adminTicketRepository;
    /**
     * WebSocket 会话令牌签发器
     */
    private final TicketWsTokenIssuer ticketWsTokenIssuer;

    /**
     * 工单元数据更新幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_PATCH = "admin_ticket_patch";
    /**
     * 工单指派幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_ASSIGN = "admin_ticket_assign";
    /**
     * 工单状态更新幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_STATUS = "admin_ticket_status";
    /**
     * 管理侧消息发送幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_MESSAGE_CREATE = "admin_msg_create";
    /**
     * 管理侧消息编辑幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_MESSAGE_EDIT = "admin_msg_edit";
    /**
     * 管理侧消息撤回幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_MESSAGE_RECALL = "admin_msg_recall";
    /**
     * 管理侧已读位点更新幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_MESSAGE_READ = "admin_msg_read";
    /**
     * 管理侧 WebSocket 会话签发幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_WS_SESSION_CREATE = "admin_ws_session_create";
    /**
     * 管理侧新增参与方幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_CREATE = "admin_participant_create";
    /**
     * 管理侧更新参与方幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_PATCH = "admin_participant_patch";
    /**
     * 管理侧参与方离场幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_LEAVE = "admin_participant_leave";

    /**
     * 撤回消息占位文案
     */
    private static final String MESSAGE_RECALLED_PLACEHOLDER = "[该消息已撤回]";
    /**
     * 默认 WebSocket 地址
     */
    private static final String DEFAULT_ADMIN_WS_URL = "ws://localhost:8080" + SecurityConstants.API_PREFIX + "/ws/customerservice";
    /**
     * WebSocket 会话令牌 TTL 秒数
     */
    private static final int WS_TOKEN_TTL_SECONDS = 1800;
    /**
     * 心跳间隔秒数
     */
    private static final int WS_HEARTBEAT_INTERVAL_SECONDS = 30;
    /**
     * 续传窗口秒数
     */
    private static final int WS_RESUME_TTL_SECONDS = 600;

    /**
     * 构造管理侧工单领域服务
     *
     * @param adminTicketRepository 管理侧工单仓储
     * @param ticketIdempotencyPort 工单幂等端口
     * @param ticketWsTokenIssuer   WebSocket 会话令牌签发器
     */
    public AdminTicketService(@NotNull IAdminTicketRepository adminTicketRepository,
                              @NotNull ITicketIdempotencyPort ticketIdempotencyPort,
                              @NotNull TicketWsTokenIssuer ticketWsTokenIssuer) {
        super(ticketIdempotencyPort);
        this.adminTicketRepository = adminTicketRepository;
        this.ticketWsTokenIssuer = ticketWsTokenIssuer;
    }

    /**
     * 分页查询管理侧工单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 工单摘要分页结果
     */
    @Override
    public @NotNull PageResult<AdminTicketSummaryView> pageTickets(@NotNull AdminTicketPageCriteria criteria,
                                                                   @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        return adminTicketRepository.pageAdminTicketSummaries(criteria, pageQuery);
    }

    /**
     * 查询管理侧工单详情
     *
     * @param ticketId 工单 ID
     * @return 工单详情视图
     */
    @Override
    public @NotNull AdminTicketDetailView getTicketDetail(@NotNull Long ticketId) {
        return adminTicketRepository.findAdminTicketDetail(ticketId)
                .orElseThrow(() -> new NotFoundException("工单不存在"));
    }

    /**
     * 更新工单元数据
     *
     * @param actorUserId           操作者用户 ID
     * @param ticketId              工单 ID
     * @param priority              工单优先级
     * @param tags                  工单标签
     * @param requestedRefundAmount 申请退款金额
     * @param currency              币种
     * @param claimExternalId       理赔外部编号
     * @param slaDueAt              SLA 到期时间
     * @param idempotencyKey        幂等键
     * @return 更新后的工单详情视图
     */
    @Override
    public @NotNull AdminTicketDetailView patchTicket(@NotNull Long actorUserId,
                                                      @NotNull Long ticketId,
                                                      @Nullable TicketPriority priority,
                                                      @Nullable List<String> tags,
                                                      @Nullable Long requestedRefundAmount,
                                                      @Nullable String currency,
                                                      @Nullable String claimExternalId,
                                                      @Nullable LocalDateTime slaDueAt,
                                                      @NotNull String idempotencyKey) {
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_PATCH,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                "相同幂等键的元数据更新请求正在处理中",
                resultRef -> getTicketDetail(ticketId),
                () -> {
                    CustomerServiceTicket ticket = requireTicket(ticketId);
                    LocalDateTime expectedUpdatedAt = normalizeNotNull(ticket.getUpdatedAt(), "updatedAt 不能为空");
                    ticket.patch(priority, tags, requestedRefundAmount, currency, claimExternalId, slaDueAt);

                    boolean updated = adminTicketRepository.updateTicketMetadataWithCas(ticket, expectedUpdatedAt);
                    if (!updated) {
                        AdminTicketDetailView latest = getTicketDetail(ticketId);
                        if (metadataAlreadyApplied(latest, priority, tags, requestedRefundAmount, currency, claimExternalId, slaDueAt))
                            return latest;
                        throw new ConflictException("工单已被其他请求更新, 请刷新后重试");
                    }
                    return getTicketDetail(ticketId);
                },
                result -> String.valueOf(result.ticketId())
        );
    }

    /**
     * 指派或转派工单
     *
     * @param actorUserId      操作者用户 ID
     * @param ticketId         工单 ID
     * @param toAssigneeUserId 目标指派坐席用户 ID
     * @param actionType       指派动作
     * @param note             备注
     * @param sourceRef        来源引用 ID
     * @param idempotencyKey   幂等键
     * @return 更新后的工单详情视图
     */
    @Override
    public @NotNull AdminTicketDetailView assignTicket(@NotNull Long actorUserId,
                                                       @NotNull Long ticketId,
                                                       @Nullable Long toAssigneeUserId,
                                                       @NotNull TicketAssignmentActionType actionType,
                                                       @Nullable String note,
                                                       @Nullable String sourceRef,
                                                       @NotNull String idempotencyKey) {
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_ASSIGN,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                "相同幂等键的指派请求正在处理中",
                resultRef -> getTicketDetail(ticketId),
                () -> {
                    CustomerServiceTicket ticket = requireTicket(ticketId);
                    LocalDateTime expectedUpdatedAt = normalizeNotNull(ticket.getUpdatedAt(), "updatedAt 不能为空");
                    String normalizedSourceRef = sourceRef == null || sourceRef.isBlank()
                            ? buildSourceRef("admin:ticket:assign:" + ticketId + ":", idempotencyKey)
                            : sourceRef;
                    TicketAssignmentLog assignmentLog = ticket.assign(
                            toAssigneeUserId,
                            actionType,
                            TicketActorType.AGENT,
                            actorUserId,
                            normalizedSourceRef,
                            note
                    );

                    boolean updated = adminTicketRepository.updateTicketAssignmentWithCasAndAppendLog(
                            ticket,
                            expectedUpdatedAt,
                            assignmentLog
                    );
                    if (!updated) {
                        AdminTicketDetailView latest = getTicketDetail(ticketId);
                        if (assignmentAlreadyApplied(latest, toAssigneeUserId, actionType))
                            return latest;
                        throw new ConflictException("工单已被其他请求更新, 请刷新后重试");
                    }
                    return getTicketDetail(ticketId);
                },
                result -> String.valueOf(result.ticketId())
        );
    }

    /**
     * 推进工单状态
     *
     * @param actorUserId    操作者用户 ID
     * @param ticketId       工单 ID
     * @param toStatus       目标状态
     * @param note           备注
     * @param sourceRef      来源引用 ID
     * @param idempotencyKey 幂等键
     * @return 更新后的工单详情视图
     */
    @Override
    public @NotNull AdminTicketDetailView transitionTicketStatus(@NotNull Long actorUserId,
                                                                 @NotNull Long ticketId,
                                                                 @NotNull TicketStatus toStatus,
                                                                 @Nullable String note,
                                                                 @Nullable String sourceRef,
                                                                 @NotNull String idempotencyKey) {
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_STATUS,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                "相同幂等键的状态更新请求正在处理中",
                resultRef -> getTicketDetail(ticketId),
                () -> {
                    CustomerServiceTicket ticket = requireTicket(ticketId);
                    Long persistedTicketId = requirePersistedTicketId(ticket);
                    TicketParticipant participant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
                    if (!participant.getRole().canTransitStatus())
                        throw new ConflictException("当前角色不允许变更工单状态");

                    TicketStatus expectedFromStatus = ticket.getStatus();
                    String normalizedSourceRef = sourceRef == null || sourceRef.isBlank()
                            ? buildSourceRef("admin:ticket:status:" + ticketId + ":", idempotencyKey)
                            : sourceRef;
                    TicketStatusLog statusLog = ticket.transitionStatus(
                            toStatus,
                            TicketActorType.AGENT,
                            actorUserId,
                            normalizedSourceRef,
                            note
                    );

                    boolean updated = adminTicketRepository.updateTicketStatusWithCasAndAppendLog(ticket, expectedFromStatus, statusLog);
                    if (!updated) {
                        CustomerServiceTicket latest = requireTicket(ticketId);
                        if (latest.getStatus() == toStatus)
                            return getTicketDetail(ticketId);
                        throw new ConflictException("工单状态已变化, 请刷新后重试");
                    }
                    return getTicketDetail(ticketId);
                },
                result -> String.valueOf(result.ticketId())
        );
    }

    /**
     * 查询管理侧指定工单的消息列表
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @param beforeId    向前翻页锚点
     * @param afterId     向后补偿锚点
     * @param ascOrder    是否按升序返回
     * @param size        返回条数
     * @return 消息列表
     */
    @Override
    public @NotNull List<TicketMessageView> listTicketMessages(@NotNull Long actorUserId,
                                                               @NotNull Long ticketId,
                                                               @Nullable Long beforeId,
                                                               @Nullable Long afterId,
                                                               boolean ascOrder,
                                                               int size) {
        require(size >= 1 && size <= 100, "size 必须在 1 到 100 之间");
        if (beforeId != null)
            require(beforeId >= 1, "before_id 必须大于等于 1");
        if (afterId != null)
            require(afterId >= 1, "after_id 必须大于等于 1");
        if (beforeId != null && afterId != null)
            throw new ConflictException("before_id 与 after_id 不可同时使用");

        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);
        return adminTicketRepository.listTicketMessages(persistedTicketId, beforeId, afterId, ascOrder, size);
    }

    /**
     * 发送管理侧工单消息
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param messageType     消息类型
     * @param content         消息正文
     * @param attachments     附件列表
     * @param clientMessageId 客户端消息幂等键
     * @param idempotencyKey  请求幂等键
     * @return 发送后的消息
     */
    @Override
    public @NotNull TicketMessageView createTicketMessage(@NotNull Long actorUserId,
                                                          @NotNull Long ticketId,
                                                          @Nullable TicketMessageType messageType,
                                                          @Nullable String content,
                                                          @Nullable List<String> attachments,
                                                          @NotNull String clientMessageId,
                                                          @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);
        if (ticket.getStatus() == TicketStatus.CLOSED)
            throw new ConflictException("工单已关闭, 不允许继续发送消息");

        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_CREATE,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                "相同幂等键的发送消息请求正在处理中",
                resultRef -> {
                    if (resultRef != null && !resultRef.isBlank()) {
                        try {
                            Long messageId = Long.parseLong(resultRef);
                            return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                                    .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
                        } catch (NumberFormatException ignored) {
                            // 兼容历史 resultRef 异常值, 回退到 clientMessageId 回查
                        }
                    }
                    return adminTicketRepository.findMessageViewByClientMessageId(
                                    persistedTicketId,
                                    TicketActorStrategy.AGENT.participantType(),
                                    actorUserId,
                                    clientMessageId
                            )
                            .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
                },
                () -> {
                    TicketMessageView duplicated = adminTicketRepository.findMessageViewByClientMessageId(
                                    persistedTicketId,
                                    TicketActorStrategy.AGENT.participantType(),
                                    actorUserId,
                                    clientMessageId
                            )
                            .orElse(null);
                    if (duplicated != null)
                        return duplicated;

                    TicketMessage message = ticket.appendMessage(
                            TicketActorStrategy.AGENT.participantType(),
                            actorUserId,
                            messageType,
                            content,
                            attachments,
                            null,
                            clientMessageId
                    );
                    return adminTicketRepository.saveTicketMessageAndTouchTicket(
                            normalizeNotNull(ticket.getUserId(), "ticketUserId 不能为空"),
                            ticket,
                            message
                    );
                },
                result -> String.valueOf(result.id())
        );
    }

    /**
     * 编辑管理侧工单消息
     *
     * @param actorUserId    操作者用户 ID
     * @param ticketId       工单 ID
     * @param messageId      消息 ID
     * @param content        新正文
     * @param idempotencyKey 请求幂等键
     * @return 编辑后的消息
     */
    @Override
    public @NotNull TicketMessageView editTicketMessage(@NotNull Long actorUserId,
                                                        @NotNull Long ticketId,
                                                        @NotNull Long messageId,
                                                        @NotNull String content,
                                                        @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);

        String resource = ticketId + ":" + messageId;
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_EDIT,
                actorUserId,
                resource,
                idempotencyKey,
                "相同幂等键的编辑消息请求正在处理中",
                resultRef -> adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                        .orElseThrow(() -> new AppException("幂等编辑结果已存在, 但消息记录不存在")),
                () -> {
                    TicketMessage message = adminTicketRepository.findTicketMessageById(persistedTicketId, messageId)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                    TicketActorStrategy.AGENT.validateMessageOperator(actorUserId, message);
                    message.editContent(content);

                    boolean updated = adminTicketRepository.updateTicketMessageContentWithCas(
                            persistedTicketId,
                            messageId,
                            normalizeNotNull(message.getContent(), "content 不能为空"),
                            normalizeNotNull(message.getEditedAt(), "editedAt 不能为空"),
                            normalizeNotNull(message.getUpdatedAt(), "updatedAt 不能为空")
                    );
                    if (!updated) {
                        TicketMessage latest = adminTicketRepository.findTicketMessageById(persistedTicketId, messageId)
                                .orElseThrow(() -> new NotFoundException("消息不存在"));
                        if (latest.getRecalledAt() != null)
                            throw new ConflictException("消息已撤回, 不允许编辑");
                        if (Objects.equals(latest.getContent(), content))
                            return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                                    .orElseThrow(() -> new NotFoundException("消息不存在"));
                        throw new ConflictException("消息已变化, 请刷新后重试");
                    }
                    return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                },
                result -> String.valueOf(result.id())
        );
    }

    /**
     * 撤回管理侧工单消息
     *
     * @param actorUserId    操作者用户 ID
     * @param ticketId       工单 ID
     * @param messageId      消息 ID
     * @param reason         撤回原因
     * @param idempotencyKey 请求幂等键
     * @return 撤回后的消息
     */
    @Override
    public @NotNull TicketMessageView recallTicketMessage(@NotNull Long actorUserId,
                                                          @NotNull Long ticketId,
                                                          @NotNull Long messageId,
                                                          @Nullable String reason,
                                                          @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);

        String resource = ticketId + ":" + messageId;
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_RECALL,
                actorUserId,
                resource,
                idempotencyKey,
                "相同幂等键的撤回消息请求正在处理中",
                resultRef -> adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                        .orElseThrow(() -> new AppException("幂等撤回结果已存在, 但消息记录不存在")),
                () -> {
                    TicketMessage message = adminTicketRepository.findTicketMessageById(persistedTicketId, messageId)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                    TicketActorStrategy.AGENT.validateMessageOperator(actorUserId, message);
                    message.recall();

                    String recalledContent = reason == null || reason.isBlank()
                            ? MESSAGE_RECALLED_PLACEHOLDER
                            : MESSAGE_RECALLED_PLACEHOLDER + "(" + reason.strip() + ")";
                    boolean updated = adminTicketRepository.recallTicketMessageWithCas(
                            persistedTicketId,
                            messageId,
                            recalledContent,
                            normalizeNotNull(message.getRecalledAt(), "recalledAt 不能为空"),
                            normalizeNotNull(message.getUpdatedAt(), "updatedAt 不能为空")
                    );
                    if (!updated) {
                        TicketMessage latest = adminTicketRepository.findTicketMessageById(persistedTicketId, messageId)
                                .orElseThrow(() -> new NotFoundException("消息不存在"));
                        if (latest.getRecalledAt() != null)
                            return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                                    .orElseThrow(() -> new NotFoundException("消息不存在"));
                        throw new ConflictException("消息已变化, 请刷新后重试");
                    }
                    return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                },
                result -> String.valueOf(result.id())
        );
    }

    /**
     * 标记管理侧工单消息已读位点
     *
     * @param actorUserId       操作者用户 ID
     * @param ticketId          工单 ID
     * @param lastReadMessageId 最后已读消息 ID
     * @param idempotencyKey    请求幂等键
     * @return 已读位点更新结果
     */
    @Override
    public @NotNull TicketReadUpdateView markTicketRead(@NotNull Long actorUserId,
                                                        @NotNull Long ticketId,
                                                        @NotNull Long lastReadMessageId,
                                                        @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);

        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_READ,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                "相同幂等键的已读请求正在处理中",
                resultRef -> {
                    TicketParticipant participant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
                    return TicketReadUpdateViewMapper.toReadUpdateView(persistedTicketId, participant);
                },
                () -> {
                    require(adminTicketRepository.existsMessageInTicket(persistedTicketId, lastReadMessageId), "lastReadMessageId 不属于当前工单");
                    TicketParticipant participant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
                    participant.markRead(lastReadMessageId, null);

                    boolean updated = adminTicketRepository.updateParticipantReadWithCas(
                            normalizeNotNull(participant.getId(), "participantId 不能为空"),
                            persistedTicketId,
                            normalizeNotNull(participant.getLastReadMessageId(), "lastReadMessageId 不能为空"),
                            normalizeNotNull(participant.getLastReadAt(), "lastReadAt 不能为空"),
                            normalizeNotNull(participant.getUpdatedAt(), "updatedAt 不能为空")
                    );
                    if (!updated) {
                        TicketParticipant latest = requireActiveAgentParticipant(persistedTicketId, actorUserId);
                        Long latestReadMessageId = latest.getLastReadMessageId();
                        if (latestReadMessageId != null && latestReadMessageId >= lastReadMessageId)
                            return TicketReadUpdateViewMapper.toReadUpdateView(persistedTicketId, latest);
                        throw new ConflictException("已读位点已变化, 请刷新后重试");
                    }
                    return TicketReadUpdateViewMapper.toReadUpdateView(persistedTicketId, participant);
                },
                result -> String.valueOf(result.lastReadMessageId())
        );
    }

    /**
     * 查询管理侧工单参与方列表
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @return 参与方列表
     */
    @Override
    public @NotNull List<TicketParticipant> listTicketParticipants(@NotNull Long actorUserId,
                                                                   @NotNull Long ticketId) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);
        return adminTicketRepository.listTicketParticipants(persistedTicketId);
    }

    /**
     * 新增管理侧工单参与方
     *
     * @param actorUserId       操作者用户 ID
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     * @param idempotencyKey    请求幂等键
     * @return 新增后的参与方
     */
    @Override
    public @NotNull TicketParticipant createTicketParticipant(@NotNull Long actorUserId,
                                                              @NotNull Long ticketId,
                                                              @NotNull TicketParticipantType participantType,
                                                              @Nullable Long participantUserId,
                                                              @NotNull TicketParticipantRole role,
                                                              @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        TicketParticipant actorParticipant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
        requireParticipantManagePermission(actorParticipant);
        validateParticipantCreateRole(ticket, participantType, participantUserId, role);

        String resource = String.valueOf(ticketId);
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_CREATE,
                actorUserId,
                resource,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded()) {
            String resultRef = tokenStatus.ticketNo();
            if (resultRef != null && !resultRef.isBlank()) {
                try {
                    Long participantId = Long.parseLong(resultRef);
                    return adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                            .orElseThrow(() -> new AppException("幂等参与方创建结果已存在, 但参与方记录不存在"));
                } catch (NumberFormatException ignored) {
                    return adminTicketRepository.findActiveParticipant(persistedTicketId, participantType, participantUserId)
                            .orElseThrow(() -> new AppException("幂等参与方创建结果已存在, 但参与方记录不存在"));
                }
            }
            return adminTicketRepository.findActiveParticipant(persistedTicketId, participantType, participantUserId)
                    .orElseThrow(() -> new AppException("幂等参与方创建结果已存在, 但参与方记录不存在"));
        }
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的新增参与方请求正在处理中");

        TicketParticipant existed = adminTicketRepository.findActiveParticipant(persistedTicketId, participantType, participantUserId)
                .orElse(null);
        if (existed != null) {
            if (existed.getRole() == role) {
                markActionSucceeded(
                        IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_CREATE,
                        actorUserId,
                        resource,
                        idempotencyKey,
                        String.valueOf(normalizeNotNull(existed.getId(), "participantId 不能为空"))
                );
                return existed;
            }
            throw new ConflictException("同类型同用户的活跃参与方已存在");
        }

        TicketParticipant created = ticket.addParticipant(participantType, participantUserId, role);
        TicketParticipant persisted = adminTicketRepository.saveTicketParticipant(created);
        markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_CREATE,
                actorUserId,
                resource,
                idempotencyKey,
                String.valueOf(normalizeNotNull(persisted.getId(), "participantId 不能为空"))
        );
        return persisted;
    }

    /**
     * 更新管理侧工单参与方角色
     *
     * @param actorUserId    操作者用户 ID
     * @param ticketId       工单 ID
     * @param participantId  参与方 ID
     * @param role           目标角色
     * @param idempotencyKey 请求幂等键
     * @return 更新后的参与方
     */
    @Override
    public @NotNull TicketParticipant patchTicketParticipant(@NotNull Long actorUserId,
                                                             @NotNull Long ticketId,
                                                             @NotNull Long participantId,
                                                             @NotNull TicketParticipantRole role,
                                                             @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        TicketParticipant actorParticipant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
        requireParticipantManagePermission(actorParticipant);

        String resource = ticketId + ":" + participantId;
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_PATCH,
                actorUserId,
                resource,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                    .orElseThrow(() -> new AppException("幂等参与方更新结果已存在, 但参与方记录不存在"));
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的参与方更新请求正在处理中");

        TicketParticipant participant = adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                .orElseThrow(() -> new NotFoundException("参与方不存在"));
        validateParticipantPatchRole(participant, role);

        if (participant.getRole() == role) {
            markActionSucceeded(
                    IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_PATCH,
                    actorUserId,
                    resource,
                    idempotencyKey,
                    String.valueOf(participantId)
            );
            return participant;
        }

        LocalDateTime expectedUpdatedAt = normalizeNotNull(participant.getUpdatedAt(), "updatedAt 不能为空");
        participant.changeRole(role);
        boolean updated = adminTicketRepository.updateTicketParticipantRoleWithCas(
                participantId,
                persistedTicketId,
                role,
                expectedUpdatedAt
        );
        if (!updated) {
            TicketParticipant latest = adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                    .orElseThrow(() -> new NotFoundException("参与方不存在"));
            if (!latest.isActive())
                throw new ConflictException("参与方已离开会话, 不允许更新角色");
            if (latest.getRole() == role) {
                markActionSucceeded(
                        IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_PATCH,
                        actorUserId,
                        resource,
                        idempotencyKey,
                        String.valueOf(participantId)
                );
                return latest;
            }
            throw new ConflictException("参与方角色已变化, 请刷新后重试");
        }

        TicketParticipant updatedParticipant = adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                .orElseThrow(() -> new NotFoundException("参与方不存在"));
        markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_PATCH,
                actorUserId,
                resource,
                idempotencyKey,
                String.valueOf(participantId)
        );
        return updatedParticipant;
    }

    /**
     * 将管理侧工单参与方设为离开
     *
     * @param actorUserId    操作者用户 ID
     * @param ticketId       工单 ID
     * @param participantId  参与方 ID
     * @param idempotencyKey 请求幂等键
     */
    @Override
    public void leaveTicketParticipant(@NotNull Long actorUserId,
                                       @NotNull Long ticketId,
                                       @NotNull Long participantId,
                                       @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        TicketParticipant actorParticipant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
        requireParticipantManagePermission(actorParticipant);

        String resource = ticketId + ":" + participantId;
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_LEAVE,
                actorUserId,
                resource,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return;
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的参与方离场请求正在处理中");

        TicketParticipant participant = adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                .orElseThrow(() -> new NotFoundException("参与方不存在"));
        if (!participant.isActive()) {
            markActionSucceeded(
                    IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_LEAVE,
                    actorUserId,
                    resource,
                    idempotencyKey,
                    String.valueOf(participantId)
            );
            return;
        }
        validateParticipantLeave(participant);

        LocalDateTime expectedUpdatedAt = normalizeNotNull(participant.getUpdatedAt(), "updatedAt 不能为空");
        participant.leave(null);
        boolean updated = adminTicketRepository.leaveTicketParticipantWithCas(
                participantId,
                persistedTicketId,
                normalizeNotNull(participant.getLeftAt(), "leftAt 不能为空"),
                expectedUpdatedAt
        );
        if (!updated) {
            TicketParticipant latest = adminTicketRepository.findTicketParticipantById(persistedTicketId, participantId)
                    .orElseThrow(() -> new NotFoundException("参与方不存在"));
            if (latest.getLeftAt() != null) {
                markActionSucceeded(
                        IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_LEAVE,
                        actorUserId,
                        resource,
                        idempotencyKey,
                        String.valueOf(participantId)
                );
                return;
            }
            throw new ConflictException("参与方状态已变化, 请刷新后重试");
        }

        markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_PARTICIPANT_LEAVE,
                actorUserId,
                resource,
                idempotencyKey,
                String.valueOf(participantId)
        );
    }

    /**
     * 分页查询管理侧工单状态日志
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @param pageQuery   分页参数
     * @return 状态日志分页结果
     */
    @Override
    public @NotNull PageResult<TicketStatusLog> listTicketStatusLogs(@NotNull Long actorUserId,
                                                                     @NotNull Long ticketId,
                                                                     @NotNull PageQuery pageQuery) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);
        pageQuery.validate();
        return adminTicketRepository.pageAdminTicketStatusLogs(persistedTicketId, pageQuery);
    }

    /**
     * 分页查询管理侧工单指派日志
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @param pageQuery   分页参数
     * @return 指派日志分页结果
     */
    @Override
    public @NotNull PageResult<TicketAssignmentLog> listTicketAssignmentLogs(@NotNull Long actorUserId,
                                                                             @NotNull Long ticketId,
                                                                             @NotNull PageQuery pageQuery) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);
        requireActiveAgentParticipant(persistedTicketId, actorUserId);
        pageQuery.validate();
        return adminTicketRepository.pageTicketAssignmentLogs(persistedTicketId, pageQuery);
    }

    /**
     * 创建管理侧 WebSocket 会话签发结果
     *
     * @param actorUserId    操作者用户 ID
     * @param command        会话创建命令
     * @param idempotencyKey 请求幂等键
     * @return 会话签发结果
     */
    @Override
    public @NotNull TicketWsSessionIssueView createWsSession(@NotNull Long actorUserId,
                                                             @NotNull TicketWsSessionCreateCommand command,
                                                             @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_WS_SESSION_CREATE,
                actorUserId,
                "admin",
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded()) {
            String existedToken = tokenStatus.ticketNo();
            if (existedToken != null && !existedToken.isBlank())
                return ticketWsTokenIssuer.buildSessionIssueView(
                        existedToken,
                        DEFAULT_ADMIN_WS_URL,
                        WS_TOKEN_TTL_SECONDS,
                        WS_HEARTBEAT_INTERVAL_SECONDS,
                        WS_RESUME_TTL_SECONDS
                );
        }

        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的会话签发请求正在处理中");

        if (!command.ticketNos().isEmpty()) {
            List<Long> ownedTicketIds = adminTicketRepository.listAgentTicketIdsByNos(actorUserId, command.ticketNos());
            require(ownedTicketIds.size() == command.ticketNos().size(), "存在无权限订阅的 ticket_no");
        }
        if (!command.ticketIds().isEmpty()) {
            List<Long> ownedTicketIds = adminTicketRepository.listAgentTicketIdsByIds(actorUserId, command.ticketIds());
            require(ownedTicketIds.size() == command.ticketIds().size(), "存在无权限订阅的 ticket_id");
        }

        String wsToken = ticketWsTokenIssuer.issueWsToken(actorUserId, TicketActorStrategy.AGENT, WS_TOKEN_TTL_SECONDS);
        markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_WS_SESSION_CREATE,
                actorUserId,
                "admin",
                idempotencyKey,
                wsToken
        );
        return ticketWsTokenIssuer.buildSessionIssueView(
                wsToken,
                DEFAULT_ADMIN_WS_URL,
                WS_TOKEN_TTL_SECONDS,
                WS_HEARTBEAT_INTERVAL_SECONDS,
                WS_RESUME_TTL_SECONDS
        );
    }

    /**
     * 查询工单聚合, 不存在时抛出异常
     *
     * @param ticketId 工单 ID
     * @return 工单聚合
     */
    private @NotNull CustomerServiceTicket requireTicket(@NotNull Long ticketId) {
        return adminTicketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new NotFoundException("工单不存在"));
    }

    /**
     * 校验工单已持久化并返回主键
     *
     * @param ticket 工单聚合
     * @return 工单主键
     */
    private @NotNull Long requirePersistedTicketId(@NotNull CustomerServiceTicket ticket) {
        Long ticketId = normalizeNotNull(ticket.getId(), "ticketId 不能为空");
        require(ticketId > 0, "ticketId 必须大于 0");
        return ticketId;
    }

    /**
     * 校验当前坐席是工单活跃参与方
     *
     * @param ticketId    工单 ID
     * @param actorUserId 操作者用户 ID
     * @return 活跃参与方
     */
    private @NotNull TicketParticipant requireActiveAgentParticipant(@NotNull Long ticketId,
                                                                     @NotNull Long actorUserId) {
        return adminTicketRepository.findActiveParticipant(ticketId, TicketParticipantType.AGENT, actorUserId)
                .orElseThrow(() -> new ConflictException("当前坐席不是工单活跃参与方"));
    }

    /**
     * 校验操作者具备参与方管理权限
     *
     * @param actorParticipant 操作者参与方实体
     */
    private void requireParticipantManagePermission(@NotNull TicketParticipant actorParticipant) {
        if (!actorParticipant.getRole().canTransitStatus())
            throw new ConflictException("当前角色不允许管理工单参与方");
    }

    /**
     * 校验新增参与方的角色组合是否合法
     *
     * @param ticket            工单聚合
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     */
    private void validateParticipantCreateRole(@NotNull CustomerServiceTicket ticket,
                                               @NotNull TicketParticipantType participantType,
                                               @Nullable Long participantUserId,
                                               @NotNull TicketParticipantRole role) {
        if (role == TicketParticipantRole.ASSIGNEE)
            throw new ConflictException("ASSIGNEE 角色请通过指派接口维护");
        if (participantType == TicketParticipantType.USER) {
            if (role != TicketParticipantRole.OWNER)
                throw new ConflictException("USER 类型参与方角色只能为 OWNER");
            Long ticketOwnerUserId = normalizeNotNull(ticket.getUserId(), "ticketOwnerUserId 不能为空");
            if (!Objects.equals(ticketOwnerUserId, participantUserId))
                throw new ConflictException("USER 类型参与方必须绑定工单所属用户");
            return;
        }
        if (role == TicketParticipantRole.OWNER)
            throw new ConflictException("仅 USER 类型参与方允许 OWNER 角色");
    }

    /**
     * 校验参与方角色更新是否合法
     *
     * @param participant 参与方实体
     * @param targetRole  目标角色
     */
    private void validateParticipantPatchRole(@NotNull TicketParticipant participant,
                                              @NotNull TicketParticipantRole targetRole) {
        if (!participant.isActive())
            throw new ConflictException("参与方已离开会话, 不允许更新角色");
        if (participant.getParticipantType() == TicketParticipantType.USER)
            throw new ConflictException("USER 类型参与方角色不可修改");
        if (participant.getRole() == TicketParticipantRole.ASSIGNEE || targetRole == TicketParticipantRole.ASSIGNEE)
            throw new ConflictException("ASSIGNEE 角色请通过指派接口维护");
        if (targetRole == TicketParticipantRole.OWNER)
            throw new ConflictException("仅 USER 类型参与方允许 OWNER 角色");
    }

    /**
     * 校验参与方离场操作是否合法
     *
     * @param participant 参与方实体
     */
    private void validateParticipantLeave(@NotNull TicketParticipant participant) {
        if (participant.getRole() == TicketParticipantRole.OWNER)
            throw new ConflictException("OWNER 参与方不允许离开工单");
        if (participant.getRole() == TicketParticipantRole.ASSIGNEE)
            throw new ConflictException("ASSIGNEE 参与方请先通过指派接口取消指派");
    }

    /**
     * 判断元数据变更是否已由其他并发请求生效
     *
     * @param detailView            最新工单详情
     * @param priority              工单优先级
     * @param tags                  工单标签
     * @param requestedRefundAmount 申请退款金额
     * @param currency              币种
     * @param claimExternalId       理赔外部编号
     * @param slaDueAt              SLA 到期时间
     * @return 是否已生效
     */
    private boolean metadataAlreadyApplied(@NotNull AdminTicketDetailView detailView,
                                           @Nullable TicketPriority priority,
                                           @Nullable List<String> tags,
                                           @Nullable Long requestedRefundAmount,
                                           @Nullable String currency,
                                           @Nullable String claimExternalId,
                                           @Nullable LocalDateTime slaDueAt) {
        if (priority != null && detailView.priority() != priority)
            return false;
        if (tags != null && !Objects.equals(detailView.tags(), tags))
            return false;
        if (requestedRefundAmount != null && !Objects.equals(detailView.requestedRefundAmount(), requestedRefundAmount))
            return false;
        if (currency != null) {
            String normalizedCurrency = currency.strip().toUpperCase(Locale.ROOT);
            if (!Objects.equals(detailView.currency(), normalizedCurrency))
                return false;
        }
        if (claimExternalId != null && !Objects.equals(detailView.claimExternalId(), claimExternalId))
            return false;
        return slaDueAt == null || Objects.equals(detailView.slaDueAt(), slaDueAt);
    }

    /**
     * 判断指派结果是否已由其他并发请求生效
     *
     * @param detailView       最新工单详情
     * @param toAssigneeUserId 目标指派坐席用户 ID
     * @param actionType       指派动作
     * @return 是否已生效
     */
    private boolean assignmentAlreadyApplied(@NotNull AdminTicketDetailView detailView,
                                             @Nullable Long toAssigneeUserId,
                                             @NotNull TicketAssignmentActionType actionType) {
        if (actionType == TicketAssignmentActionType.UNASSIGN)
            return detailView.assignedToUserId() == null;
        return Objects.equals(detailView.assignedToUserId(), toAssigneeUserId);
    }

    /**
     * 构造来源引用标识
     *
     * @param prefix 来源前缀
     * @param tail   来源尾部
     * @return 截断后的来源引用标识
     */
    private @NotNull String buildSourceRef(@NotNull String prefix, @NotNull String tail) {
        int maxLength = 128;
        if (prefix.length() >= maxLength)
            return prefix.substring(0, maxLength);

        int remain = maxLength - prefix.length();
        String normalizedTail = tail.length() <= remain ? tail : tail.substring(0, remain);
        return prefix + normalizedTail;
    }
}
