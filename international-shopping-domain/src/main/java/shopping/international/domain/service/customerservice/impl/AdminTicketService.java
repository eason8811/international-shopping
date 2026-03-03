package shopping.international.domain.service.customerservice.impl;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
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
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.domain.model.vo.user.JwtIssueSpec;
import shopping.international.domain.service.customerservice.IAdminTicketService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNull;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 管理侧工单领域服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminTicketService implements IAdminTicketService {

    /**
     * 管理侧工单仓储
     */
    private final IAdminTicketRepository adminTicketRepository;
    /**
     * 工单幂等端口
     */
    private final ITicketIdempotencyPort ticketIdempotencyPort;
    /**
     * JWT 签发配置
     */
    private final JwtIssueSpec jwtIssueSpec;

    /**
     * 幂等占位状态 TTL
     */
    private static final Duration IDEMPOTENCY_PENDING_TTL = Duration.ofMinutes(5);
    /**
     * 幂等成功状态 TTL
     */
    private static final Duration IDEMPOTENCY_SUCCESS_TTL = Duration.ofHours(24);

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
     * @param actorUserId            操作者用户 ID
     * @param ticketId               工单 ID
     * @param priority               工单优先级
     * @param tags                   工单标签
     * @param requestedRefundAmount  申请退款金额
     * @param currency               币种
     * @param claimExternalId        理赔外部编号
     * @param slaDueAt               SLA 到期时间
     * @param idempotencyKey         幂等键
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
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_PATCH,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return getTicketDetail(ticketId);
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的元数据更新请求正在处理中");

        CustomerServiceTicket ticket = requireTicket(ticketId);
        LocalDateTime expectedUpdatedAt = normalizeNotNull(ticket.getUpdatedAt(), "updatedAt 不能为空");
        ticket.patch(priority, tags, requestedRefundAmount, currency, claimExternalId, slaDueAt);

        boolean updated = adminTicketRepository.updateTicketMetadataWithCas(ticket, expectedUpdatedAt);
        if (!updated) {
            AdminTicketDetailView latest = getTicketDetail(ticketId);
            if (metadataAlreadyApplied(latest, priority, tags, requestedRefundAmount, currency, claimExternalId, slaDueAt)) {
                markSucceeded(IDEMPOTENCY_SCENE_ADMIN_PATCH, actorUserId, ticketId, idempotencyKey);
                return latest;
            }
            throw new ConflictException("工单已被其他请求更新, 请刷新后重试");
        }

        AdminTicketDetailView detailView = getTicketDetail(ticketId);
        markSucceeded(IDEMPOTENCY_SCENE_ADMIN_PATCH, actorUserId, ticketId, idempotencyKey);
        return detailView;
    }

    /**
     * 指派或转派工单
     *
     * @param actorUserId       操作者用户 ID
     * @param ticketId          工单 ID
     * @param toAssigneeUserId  目标指派坐席用户 ID
     * @param actionType        指派动作
     * @param note              备注
     * @param sourceRef         来源引用 ID
     * @param idempotencyKey    幂等键
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
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_ASSIGN,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return getTicketDetail(ticketId);
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的指派请求正在处理中");

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
            if (assignmentAlreadyApplied(latest, toAssigneeUserId, actionType)) {
                markSucceeded(IDEMPOTENCY_SCENE_ADMIN_ASSIGN, actorUserId, ticketId, idempotencyKey);
                return latest;
            }
            throw new ConflictException("工单已被其他请求更新, 请刷新后重试");
        }

        AdminTicketDetailView detailView = getTicketDetail(ticketId);
        markSucceeded(IDEMPOTENCY_SCENE_ADMIN_ASSIGN, actorUserId, ticketId, idempotencyKey);
        return detailView;
    }

    /**
     * 推进工单状态
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param toStatus        目标状态
     * @param note            备注
     * @param sourceRef       来源引用 ID
     * @param idempotencyKey  幂等键
     * @return 更新后的工单详情视图
     */
    @Override
    public @NotNull AdminTicketDetailView transitionTicketStatus(@NotNull Long actorUserId,
                                                                 @NotNull Long ticketId,
                                                                 @NotNull TicketStatus toStatus,
                                                                 @Nullable String note,
                                                                 @Nullable String sourceRef,
                                                                 @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_STATUS,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return getTicketDetail(ticketId);
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的状态更新请求正在处理中");

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
            if (latest.getStatus() == toStatus) {
                AdminTicketDetailView detailView = getTicketDetail(ticketId);
                markSucceeded(IDEMPOTENCY_SCENE_ADMIN_STATUS, actorUserId, ticketId, idempotencyKey);
                return detailView;
            }
            throw new ConflictException("工单状态已变化, 请刷新后重试");
        }

        AdminTicketDetailView detailView = getTicketDetail(ticketId);
        markSucceeded(IDEMPOTENCY_SCENE_ADMIN_STATUS, actorUserId, ticketId, idempotencyKey);
        return detailView;
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
     * @param actorUserId      操作者用户 ID
     * @param ticketId         工单 ID
     * @param messageType      消息类型
     * @param content          消息正文
     * @param attachments      附件列表
     * @param clientMessageId  客户端消息幂等键
     * @param idempotencyKey   请求幂等键
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

        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_CREATE,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded()) {
            String resultRef = tokenStatus.ticketNo();
            if (resultRef != null && !resultRef.isBlank()) {
                try {
                    Long messageId = Long.parseLong(resultRef);
                    return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                            .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
                } catch (NumberFormatException ignored) {
                    return adminTicketRepository.findMessageViewByClientMessageId(
                                    persistedTicketId,
                                    TicketParticipantType.AGENT,
                                    actorUserId,
                                    clientMessageId
                            )
                            .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
                }
            }
            return adminTicketRepository.findMessageViewByClientMessageId(
                            persistedTicketId,
                            TicketParticipantType.AGENT,
                            actorUserId,
                            clientMessageId
                    )
                    .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
        }
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的发送消息请求正在处理中");

        TicketMessageView duplicated = adminTicketRepository.findMessageViewByClientMessageId(
                        persistedTicketId,
                        TicketParticipantType.AGENT,
                        actorUserId,
                        clientMessageId
                )
                .orElse(null);
        if (duplicated != null) {
            ticketIdempotencyPort.markActionSucceeded(
                    IDEMPOTENCY_SCENE_ADMIN_MESSAGE_CREATE,
                    actorUserId,
                    String.valueOf(ticketId),
                    idempotencyKey,
                    String.valueOf(duplicated.id()),
                    IDEMPOTENCY_SUCCESS_TTL
            );
            return duplicated;
        }

        TicketMessage message = ticket.appendMessage(
                TicketParticipantType.AGENT,
                actorUserId,
                messageType,
                content,
                attachments,
                null,
                clientMessageId
        );
        TicketMessageView created = adminTicketRepository.saveTicketMessageAndTouchTicket(
                normalizeNotNull(ticket.getUserId(), "ticketUserId 不能为空"),
                ticket,
                message
        );
        ticketIdempotencyPort.markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_CREATE,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                String.valueOf(created.id()),
                IDEMPOTENCY_SUCCESS_TTL
        );
        return created;
    }

    /**
     * 编辑管理侧工单消息
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param messageId       消息 ID
     * @param content         新正文
     * @param idempotencyKey  请求幂等键
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
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_EDIT,
                actorUserId,
                resource,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded())
            return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                    .orElseThrow(() -> new AppException("幂等编辑结果已存在, 但消息记录不存在"));
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的编辑消息请求正在处理中");

        TicketMessage message = adminTicketRepository.findTicketMessageById(persistedTicketId, messageId)
                .orElseThrow(() -> new NotFoundException("消息不存在"));
        validateAgentMessageOperator(actorUserId, message);
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
            if (Objects.equals(latest.getContent(), content)) {
                TicketMessageView idempotentResult = adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                        .orElseThrow(() -> new NotFoundException("消息不存在"));
                ticketIdempotencyPort.markActionSucceeded(
                        IDEMPOTENCY_SCENE_ADMIN_MESSAGE_EDIT,
                        actorUserId,
                        resource,
                        idempotencyKey,
                        String.valueOf(idempotentResult.id()),
                        IDEMPOTENCY_SUCCESS_TTL
                );
                return idempotentResult;
            }
            throw new ConflictException("消息已变化, 请刷新后重试");
        }

        TicketMessageView updatedView = adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                .orElseThrow(() -> new NotFoundException("消息不存在"));
        ticketIdempotencyPort.markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_EDIT,
                actorUserId,
                resource,
                idempotencyKey,
                String.valueOf(updatedView.id()),
                IDEMPOTENCY_SUCCESS_TTL
        );
        return updatedView;
    }

    /**
     * 撤回管理侧工单消息
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param messageId       消息 ID
     * @param reason          撤回原因
     * @param idempotencyKey  请求幂等键
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
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_RECALL,
                actorUserId,
                resource,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                    .orElseThrow(() -> new AppException("幂等撤回结果已存在, 但消息记录不存在"));
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的撤回消息请求正在处理中");

        TicketMessage message = adminTicketRepository.findTicketMessageById(persistedTicketId, messageId)
                .orElseThrow(() -> new NotFoundException("消息不存在"));
        validateAgentMessageOperator(actorUserId, message);
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
            if (latest.getRecalledAt() != null) {
                TicketMessageView idempotentResult = adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                        .orElseThrow(() -> new NotFoundException("消息不存在"));
                ticketIdempotencyPort.markActionSucceeded(
                        IDEMPOTENCY_SCENE_ADMIN_MESSAGE_RECALL,
                        actorUserId,
                        resource,
                        idempotencyKey,
                        String.valueOf(idempotentResult.id()),
                        IDEMPOTENCY_SUCCESS_TTL
                );
                return idempotentResult;
            }
            throw new ConflictException("消息已变化, 请刷新后重试");
        }

        TicketMessageView recalledView = adminTicketRepository.findTicketMessageViewById(persistedTicketId, messageId)
                .orElseThrow(() -> new NotFoundException("消息不存在"));
        ticketIdempotencyPort.markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_RECALL,
                actorUserId,
                resource,
                idempotencyKey,
                String.valueOf(recalledView.id()),
                IDEMPOTENCY_SUCCESS_TTL
        );
        return recalledView;
    }

    /**
     * 标记管理侧工单消息已读位点
     *
     * @param actorUserId        操作者用户 ID
     * @param ticketId           工单 ID
     * @param lastReadMessageId  最后已读消息 ID
     * @param idempotencyKey     请求幂等键
     * @return 已读位点更新结果
     */
    @Override
    public @NotNull TicketReadUpdateView markTicketRead(@NotNull Long actorUserId,
                                                        @NotNull Long ticketId,
                                                        @NotNull Long lastReadMessageId,
                                                        @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireTicket(ticketId);
        Long persistedTicketId = requirePersistedTicketId(ticket);

        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_READ,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded()) {
            TicketParticipant participant = requireActiveAgentParticipant(persistedTicketId, actorUserId);
            return toReadUpdateView(persistedTicketId, participant);
        }
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的已读请求正在处理中");

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
            if (latestReadMessageId != null && latestReadMessageId >= lastReadMessageId) {
                TicketReadUpdateView idempotentView = toReadUpdateView(persistedTicketId, latest);
                ticketIdempotencyPort.markActionSucceeded(
                        IDEMPOTENCY_SCENE_ADMIN_MESSAGE_READ,
                        actorUserId,
                        String.valueOf(ticketId),
                        idempotencyKey,
                        String.valueOf(idempotentView.lastReadMessageId()),
                        IDEMPOTENCY_SUCCESS_TTL
                );
                return idempotentView;
            }
            throw new ConflictException("已读位点已变化, 请刷新后重试");
        }

        TicketReadUpdateView result = toReadUpdateView(persistedTicketId, participant);
        ticketIdempotencyPort.markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_MESSAGE_READ,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                String.valueOf(result.lastReadMessageId()),
                IDEMPOTENCY_SUCCESS_TTL
        );
        return result;
    }

    /**
     * 创建管理侧 WebSocket 会话签发结果
     *
     * @param actorUserId     操作者用户 ID
     * @param command         会话创建命令
     * @param idempotencyKey  请求幂等键
     * @return 会话签发结果
     */
    @Override
    public @NotNull TicketWsSessionIssueView createWsSession(@NotNull Long actorUserId,
                                                             @NotNull TicketWsSessionCreateCommand command,
                                                             @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_WS_SESSION_CREATE,
                actorUserId,
                "admin",
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded()) {
            String existedToken = tokenStatus.ticketNo();
            if (existedToken != null && !existedToken.isBlank())
                return buildWsSessionIssue(existedToken);
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

        String wsToken = generateWsToken(actorUserId);
        ticketIdempotencyPort.markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_WS_SESSION_CREATE,
                actorUserId,
                "admin",
                idempotencyKey,
                wsToken,
                IDEMPOTENCY_SUCCESS_TTL
        );
        return buildWsSessionIssue(wsToken);
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
     * @param ticketId     工单 ID
     * @param actorUserId  操作者用户 ID
     * @return 活跃参与方
     */
    private @NotNull TicketParticipant requireActiveAgentParticipant(@NotNull Long ticketId,
                                                                     @NotNull Long actorUserId) {
        return adminTicketRepository.findActiveParticipant(ticketId, TicketParticipantType.AGENT, actorUserId)
                .orElseThrow(() -> new ConflictException("当前坐席不是工单活跃参与方"));
    }

    /**
     * 校验消息操作者是否为当前坐席
     *
     * @param actorUserId 操作者用户 ID
     * @param message     消息实体
     */
    private void validateAgentMessageOperator(@NotNull Long actorUserId,
                                              @NotNull TicketMessage message) {
        if (message.getSenderType() != TicketParticipantType.AGENT)
            throw new ConflictException("当前消息不允许坐席执行该操作");
        if (!Objects.equals(message.getSenderUserId(), actorUserId))
            throw new ConflictException("无权操作该消息");
    }

    /**
     * 构建已读位点更新视图
     *
     * @param ticketId     工单 ID
     * @param participant  参与方实体
     * @return 已读位点更新视图
     */
    private @NotNull TicketReadUpdateView toReadUpdateView(@NotNull Long ticketId,
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

    /**
     * 构造 WebSocket 会话签发视图
     *
     * @param wsToken WebSocket 令牌
     * @return 会话签发视图
     */
    private @NotNull TicketWsSessionIssueView buildWsSessionIssue(@NotNull String wsToken) {
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(WS_TOKEN_TTL_SECONDS);
        return new TicketWsSessionIssueView(
                wsToken,
                DEFAULT_ADMIN_WS_URL,
                issuedAt,
                expiresAt,
                WS_HEARTBEAT_INTERVAL_SECONDS,
                WS_RESUME_TTL_SECONDS
        );
    }

    /**
     * 生成 WebSocket 会话令牌
     *
     * @param actorUserId 操作者用户 ID
     * @return 会话令牌
     */
    private @NotNull String generateWsToken(@NotNull Long actorUserId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(WS_TOKEN_TTL_SECONDS);
        try {
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(actorUserId.toString())
                    .claim("uid", actorUserId)
                    .claim("typ", "ws")
                    .claim("scope", "customerservice:ws")
                    .claim("role", "agent")
                    .claim("jti", UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt));

            if (jwtIssueSpec.issuer() != null && !jwtIssueSpec.issuer().isBlank())
                claimsBuilder.issuer(jwtIssueSpec.issuer());
            if (jwtIssueSpec.audience() != null && !jwtIssueSpec.audience().isBlank())
                claimsBuilder.audience(Collections.singletonList(jwtIssueSpec.audience()));

            SignedJWT wsJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsBuilder.build());
            wsJwt.sign(new MACSigner(hmacKey()));
            return wsJwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("WebSocket 会话令牌签发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 WebSocket 会话令牌签名所需密钥
     *
     * @return HMAC 密钥字节数组
     */
    private byte[] hmacKey() {
        requireNotBlank(jwtIssueSpec.secretBase64(), "JWT 密钥未配置");
        try {
            return Base64.getDecoder().decode(jwtIssueSpec.secretBase64().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT 密钥 Base64 格式非法", e);
        }
    }

    /**
     * 判断元数据变更是否已由其他并发请求生效
     *
     * @param detailView             最新工单详情
     * @param priority               工单优先级
     * @param tags                   工单标签
     * @param requestedRefundAmount  申请退款金额
     * @param currency               币种
     * @param claimExternalId        理赔外部编号
     * @param slaDueAt               SLA 到期时间
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
     * @param detailView        最新工单详情
     * @param toAssigneeUserId  目标指派坐席用户 ID
     * @param actionType        指派动作
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
     * 标记写操作幂等成功
     *
     * @param scene           幂等场景
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param idempotencyKey  幂等键
     */
    private void markSucceeded(@NotNull String scene,
                               @NotNull Long actorUserId,
                               @NotNull Long ticketId,
                               @NotNull String idempotencyKey) {
        ticketIdempotencyPort.markActionSucceeded(
                scene,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                String.valueOf(ticketId),
                IDEMPOTENCY_SUCCESS_TTL
        );
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
