package shopping.international.domain.service.customerservice.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;
import shopping.international.domain.adapter.repository.customerservice.IUserTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketChannel;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.domain.service.customerservice.AbstractTicketIdempotentActionService;
import shopping.international.domain.service.customerservice.impl.support.TicketActorStrategy;
import shopping.international.domain.service.customerservice.impl.support.TicketReadUpdateViewMapper;
import shopping.international.domain.service.customerservice.impl.support.TicketWsTokenIssuer;
import shopping.international.domain.service.customerservice.IUserTicketService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNull;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 用户侧工单领域服务实现
 */
@Service
public class UserTicketService extends AbstractTicketIdempotentActionService implements IUserTicketService {

    /**
     * 用户侧工单仓储
     */
    private final IUserTicketRepository userTicketRepository;
    /**
     * WebSocket 会话令牌签发器
     */
    private final TicketWsTokenIssuer ticketWsTokenIssuer;

    /**
     * 消息发送幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_MESSAGE_CREATE = "msg_create";
    /**
     * 消息编辑幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_MESSAGE_EDIT = "msg_edit";
    /**
     * 消息撤回幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_MESSAGE_RECALL = "msg_recall";
    /**
     * 已读位点更新幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_MESSAGE_READ = "msg_read";
    /**
     * WebSocket 会话签发幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_WS_SESSION_CREATE = "ws_session_create";

    /**
     * 撤回消息占位文案
     */
    private static final String MESSAGE_RECALLED_PLACEHOLDER = "[该消息已撤回]";
    /**
     * 默认 WebSocket 地址
     */
    private static final String DEFAULT_USER_WS_URL = "ws://localhost:8080" + SecurityConstants.API_PREFIX + "/ws/customerservice";
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
     * 构造用户侧工单领域服务
     *
     * @param userTicketRepository  用户侧工单仓储
     * @param ticketIdempotencyPort 工单幂等端口
     * @param ticketWsTokenIssuer   WebSocket 会话令牌签发器
     */
    public UserTicketService(@NotNull IUserTicketRepository userTicketRepository,
                             @NotNull ITicketIdempotencyPort ticketIdempotencyPort,
                             @NotNull TicketWsTokenIssuer ticketWsTokenIssuer) {
        super(ticketIdempotencyPort);
        this.userTicketRepository = userTicketRepository;
        this.ticketWsTokenIssuer = ticketWsTokenIssuer;
    }

    /**
     * 分页查询当前用户工单列表
     *
     * @param userId       当前用户 ID
     * @param pageQuery    分页参数
     * @param status       工单状态筛选
     * @param issueType    问题类型筛选
     * @param orderNo      订单号筛选
     * @param shipmentNo   物流单号筛选
     * @param createdFrom  创建时间起始
     * @param createdTo    创建时间结束
     * @return 用户工单摘要分页结果
     */
    @Override
    public @NotNull PageResult<UserTicketSummaryView> listMyTickets(@NotNull Long userId,
                                                                    @NotNull PageQuery pageQuery,
                                                                    @Nullable TicketStatus status,
                                                                    @Nullable TicketIssueType issueType,
                                                                    @Nullable String orderNo,
                                                                    @Nullable String shipmentNo,
                                                                    @Nullable LocalDateTime createdFrom,
                                                                    @Nullable LocalDateTime createdTo) {
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "created_from 不能晚于 created_to");

        return userTicketRepository.pageUserTicketSummaries(
                userId,
                pageQuery,
                status,
                issueType,
                orderNo,
                shipmentNo,
                createdFrom,
                createdTo
        );
    }

    /**
     * 创建用户工单
     *
     * @param userId          当前用户 ID
     * @param command         创建命令
     * @param idempotencyKey  幂等键
     * @return 创建结果
     */
    @Override
    public @NotNull UserTicketCreateResult createMyTicket(@NotNull Long userId,
                                                          @NotNull TicketCreateCommand command,
                                                          @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerCreateOrGet(
                userId,
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded()) {
            TicketNo ticketNo = TicketNo.of(tokenStatus.ticketNo());
            return userTicketRepository.findUserTicketCreateResult(userId, ticketNo)
                    .orElseThrow(() -> new AppException("幂等结果已存在, 但工单记录不存在"));
        }

        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的创建请求正在处理中");

        Optional<CustomerServiceTicket> duplicatedOpenTicket = userTicketRepository.findOpenTicketByDedupe(
                userId,
                command.orderId(),
                command.shipmentId(),
                command.issueType()
        );
        if (duplicatedOpenTicket.isPresent())
            throw new ConflictException("同一订单和问题类型下已存在进行中的工单");

        String sourceRef = buildSourceRef("user:ticket:create:", idempotencyKey);
        CustomerServiceTicket newTicket = CustomerServiceTicket.create(
                userId,
                command.orderId(),
                command.orderItemId(),
                command.shipmentId(),
                command.issueType(),
                command.title(),
                command.description(),
                command.attachments(),
                command.evidence(),
                command.requestedRefundAmount(),
                command.currency(),
                TicketChannel.CLIENT,
                sourceRef
        );

        UserTicketCreateResult created = userTicketRepository.saveNewTicket(newTicket, userId, sourceRef);
        idempotencyPort().markCreateSucceeded(userId, idempotencyKey, created.ticketNo(), IDEMPOTENCY_SUCCESS_TTL);
        return created;
    }

    /**
     * 查询当前用户工单详情
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @return 工单详情
     */
    @Override
    public @NotNull UserTicketDetailView getMyTicketDetail(@NotNull Long userId,
                                                           @NotNull TicketNo ticketNo) {
        return userTicketRepository.findUserTicketDetail(userId, ticketNo)
                .orElseThrow(() -> new NotFoundException("工单不存在"));
    }

    /**
     * 关闭当前用户工单
     *
     * @param userId          当前用户 ID
     * @param ticketNo        工单编号
     * @param note            关闭备注
     * @param idempotencyKey  幂等键
     * @return 关闭后的工单详情
     */
    @Override
    public @NotNull UserTicketDetailView closeMyTicket(@NotNull Long userId,
                                                       @NotNull TicketNo ticketNo,
                                                       @Nullable String note,
                                                       @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerCloseOrGet(
                userId,
                ticketNo.getValue(),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded())
            return getMyTicketDetail(userId, ticketNo);

        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的关闭请求正在处理中");

        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            UserTicketDetailView detailView = getMyTicketDetail(userId, ticketNo);
            idempotencyPort().markCloseSucceeded(userId, ticketNo.getValue(), idempotencyKey, IDEMPOTENCY_SUCCESS_TTL);
            return detailView;
        }

        TicketStatus expectedFromStatus = ticket.getStatus();
        String sourceRef = buildSourceRef("user:ticket:close:" + ticketNo.getValue() + ":", idempotencyKey);
        TicketStatusLog statusLog = ticket.transitionStatus(
                TicketStatus.CLOSED,
                TicketActorType.USER,
                userId,
                sourceRef,
                note
        );

        boolean updated = userTicketRepository.updateTicketStatusWithCasAndAppendLog(
                userId,
                ticket,
                expectedFromStatus,
                statusLog
        );

        if (!updated) {
            CustomerServiceTicket latest = requireUserTicket(userId, ticketNo);
            if (latest.getStatus() == TicketStatus.CLOSED) {
                UserTicketDetailView detailView = getMyTicketDetail(userId, ticketNo);
                idempotencyPort().markCloseSucceeded(userId, ticketNo.getValue(), idempotencyKey, IDEMPOTENCY_SUCCESS_TTL);
                return detailView;
            }
            throw new ConflictException("工单状态已变化, 请刷新后重试");
        }

        UserTicketDetailView detailView = getMyTicketDetail(userId, ticketNo);
        idempotencyPort().markCloseSucceeded(userId, ticketNo.getValue(), idempotencyKey, IDEMPOTENCY_SUCCESS_TTL);
        return detailView;
    }

    /**
     * 查询当前用户指定工单的消息列表
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @param beforeId  向前翻页锚点
     * @param afterId   向后补偿锚点
     * @param ascOrder  是否按升序返回
     * @param size      返回条数
     * @return 消息列表
     */
    @Override
    public @NotNull List<TicketMessageView> listMyTicketMessages(@NotNull Long userId,
                                                                 @NotNull TicketNo ticketNo,
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

        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        Long ticketId = requirePersistedTicketId(ticket);
        return userTicketRepository.listTicketMessages(ticketId, beforeId, afterId, ascOrder, size);
    }

    /**
     * 发送当前用户的工单消息
     *
     * @param userId           当前用户 ID
     * @param ticketNo         工单编号
     * @param messageType      消息类型
     * @param content          消息正文
     * @param attachments      附件列表
     * @param clientMessageId  客户端消息幂等键
     * @param idempotencyKey   请求幂等键
     * @return 发送后的消息
     */
    @Override
    public @NotNull TicketMessageView createMyTicketMessage(@NotNull Long userId,
                                                            @NotNull TicketNo ticketNo,
                                                            @Nullable TicketMessageType messageType,
                                                            @Nullable String content,
                                                            @Nullable List<String> attachments,
                                                            @NotNull String clientMessageId,
                                                            @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        Long ticketId = requirePersistedTicketId(ticket);
        if (ticket.getStatus() == TicketStatus.CLOSED)
            throw new ConflictException("工单已关闭, 不允许继续发送消息");

        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_MESSAGE_CREATE,
                userId,
                ticketNo.getValue(),
                idempotencyKey,
                "相同幂等键的发送消息请求正在处理中",
                resultRef -> {
                    if (resultRef != null && !resultRef.isBlank()) {
                        return userTicketRepository.findTicketMessageViewByNo(ticketId, TicketMessageNo.of(resultRef))
                                .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
                    }
                    return userTicketRepository.findMessageViewByClientMessageId(
                                    ticketId,
                                    TicketActorStrategy.USER.participantType(),
                                    userId,
                                    clientMessageId
                            )
                            .orElseThrow(() -> new AppException("幂等消息结果已存在, 但消息记录不存在"));
                },
                () -> {
                    Optional<TicketMessageView> duplicatedByClientMessageId = userTicketRepository.findMessageViewByClientMessageId(
                            ticketId,
                            TicketActorStrategy.USER.participantType(),
                            userId,
                            clientMessageId
                    );
                    if (duplicatedByClientMessageId.isPresent())
                        return duplicatedByClientMessageId.get();

                    TicketMessage message = ticket.appendMessage(
                            TicketActorStrategy.USER.participantType(),
                            userId,
                            messageType,
                            content,
                            attachments,
                            null,
                            clientMessageId
                    );
                    return userTicketRepository.saveTicketMessageAndTouchTicket(userId, ticket, message);
                },
                TicketMessageView::messageNo
        );
    }

    /**
     * 编辑当前用户的工单消息
     *
     * @param userId          当前用户 ID
     * @param ticketNo        工单编号
     * @param messageNo       消息编号
     * @param content         新正文
     * @param idempotencyKey  请求幂等键
     * @return 编辑后的消息
     */
    @Override
    public @NotNull TicketMessageView editMyTicketMessage(@NotNull Long userId,
                                                          @NotNull TicketNo ticketNo,
                                                          @NotNull TicketMessageNo messageNo,
                                                          @NotNull String content,
                                                          @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        Long ticketId = requirePersistedTicketId(ticket);

        String resource = ticketNo.getValue() + ":" + messageNo.getValue();
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_MESSAGE_EDIT,
                userId,
                resource,
                idempotencyKey,
                "相同幂等键的编辑消息请求正在处理中",
                resultRef -> userTicketRepository.findTicketMessageViewByNo(ticketId, messageNo)
                        .orElseThrow(() -> new AppException("幂等编辑结果已存在, 但消息记录不存在")),
                () -> {
                    TicketMessage message = userTicketRepository.findTicketMessageByNo(ticketId, messageNo)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                    TicketActorStrategy.USER.validateMessageOperator(userId, message);
                    message.editContent(content);

                    boolean updated = userTicketRepository.updateTicketMessageContentWithCas(
                            ticketId,
                            normalizeNotNull(message.getId(), "messageId 不能为空"),
                            normalizeNotNull(message.getContent(), "content 不能为空"),
                            normalizeNotNull(message.getEditedAt(), "editedAt 不能为空"),
                            message.getUpdatedAt()
                    );

                    if (!updated) {
                        TicketMessage latest = userTicketRepository.findTicketMessageByNo(ticketId, messageNo)
                                .orElseThrow(() -> new NotFoundException("消息不存在"));
                        if (latest.getRecalledAt() != null)
                            throw new ConflictException("消息已撤回, 不允许编辑");
                        if (Objects.equals(latest.getContent(), content))
                            return userTicketRepository.findTicketMessageViewByNo(ticketId, messageNo)
                                    .orElseThrow(() -> new NotFoundException("消息不存在"));
                        throw new ConflictException("消息已变化, 请刷新后重试");
                    }
                    return userTicketRepository.findTicketMessageViewByNo(ticketId, messageNo)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                },
                TicketMessageView::messageNo
        );
    }

    /**
     * 撤回当前用户的工单消息
     *
     * @param userId          当前用户 ID
     * @param ticketNo        工单编号
     * @param messageNo       消息编号
     * @param reason          撤回原因
     * @param idempotencyKey  请求幂等键
     * @return 撤回后的消息
     */
    @Override
    public @NotNull TicketMessageView recallMyTicketMessage(@NotNull Long userId,
                                                            @NotNull TicketNo ticketNo,
                                                            @NotNull TicketMessageNo messageNo,
                                                            @Nullable String reason,
                                                            @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        Long ticketId = requirePersistedTicketId(ticket);

        String resource = ticketNo.getValue() + ":" + messageNo.getValue();
        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_MESSAGE_RECALL,
                userId,
                resource,
                idempotencyKey,
                "相同幂等键的撤回消息请求正在处理中",
                resultRef -> userTicketRepository.findTicketMessageViewByNo(ticketId, messageNo)
                        .orElseThrow(() -> new AppException("幂等撤回结果已存在, 但消息记录不存在")),
                () -> {
                    TicketMessage message = userTicketRepository.findTicketMessageByNo(ticketId, messageNo)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                    TicketActorStrategy.USER.validateMessageOperator(userId, message);
                    message.recall();

                    String recalledContent = reason == null || reason.isBlank()
                            ? MESSAGE_RECALLED_PLACEHOLDER
                            : MESSAGE_RECALLED_PLACEHOLDER + "(" + reason.strip() + ")";

                    boolean updated = userTicketRepository.recallTicketMessageWithCas(
                            ticketId,
                            normalizeNotNull(message.getId(), "messageId 不能为空"),
                            recalledContent,
                            normalizeNotNull(message.getRecalledAt(), "recalledAt 不能为空"),
                            message.getUpdatedAt()
                    );

                    if (!updated) {
                        TicketMessage latest = userTicketRepository.findTicketMessageByNo(ticketId, messageNo)
                                .orElseThrow(() -> new NotFoundException("消息不存在"));
                        if (latest.getRecalledAt() != null)
                            return userTicketRepository.findTicketMessageViewByNo(ticketId, messageNo)
                                    .orElseThrow(() -> new NotFoundException("消息不存在"));
                        throw new ConflictException("消息已变化, 请刷新后重试");
                    }
                    return userTicketRepository.findTicketMessageViewByNo(ticketId, messageNo)
                            .orElseThrow(() -> new NotFoundException("消息不存在"));
                },
                TicketMessageView::messageNo
        );
    }

    /**
     * 标记当前用户在工单下的消息已读位点
     *
     * @param userId             当前用户 ID
     * @param ticketNo           工单编号
     * @param lastReadMessageId  最后已读消息 ID
     * @param idempotencyKey     请求幂等键
     * @return 已读位点更新结果
     */
    @Override
    public @NotNull TicketReadUpdateView markMyTicketRead(@NotNull Long userId,
                                                          @NotNull TicketNo ticketNo,
                                                          @NotNull Long lastReadMessageId,
                                                          @NotNull String idempotencyKey) {
        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        Long ticketId = requirePersistedTicketId(ticket);

        return executeActionWithIdempotency(
                IDEMPOTENCY_SCENE_MESSAGE_READ,
                userId,
                ticketNo.getValue(),
                idempotencyKey,
                "相同幂等键的已读请求正在处理中",
                resultRef -> {
                    TicketParticipant participant = userTicketRepository.findActiveParticipant(
                                    ticketId,
                                    TicketActorStrategy.USER.participantType(),
                                    userId
                            )
                            .orElseThrow(() -> new AppException("幂等已读结果已存在, 但参与方记录不存在"));
                    return TicketReadUpdateViewMapper.toReadUpdateView(ticketId, participant);
                },
                () -> {
                    require(userTicketRepository.existsMessageInTicket(ticketId, lastReadMessageId), "lastReadMessageId 不属于当前工单");

                    TicketParticipant participant = userTicketRepository.findActiveParticipant(
                                    ticketId,
                                    TicketActorStrategy.USER.participantType(),
                                    userId
                            )
                            .orElseThrow(() -> new ConflictException("当前用户不是工单活跃参与方"));
                    participant.markRead(lastReadMessageId, null);

                    boolean updated = userTicketRepository.updateParticipantReadWithCas(
                            normalizeNotNull(participant.getId(), "participantId 不能为空"),
                            ticketId,
                            normalizeNotNull(participant.getLastReadMessageId(), "lastReadMessageId 不能为空"),
                            normalizeNotNull(participant.getLastReadAt(), "lastReadAt 不能为空"),
                            participant.getUpdatedAt()
                    );

                    if (!updated) {
                        TicketParticipant latest = userTicketRepository.findActiveParticipant(
                                        ticketId,
                                        TicketActorStrategy.USER.participantType(),
                                        userId
                                )
                                .orElseThrow(() -> new ConflictException("当前用户不是工单活跃参与方"));
                        Long latestReadMessageId = latest.getLastReadMessageId();
                        if (latestReadMessageId != null && latestReadMessageId >= lastReadMessageId)
                            return TicketReadUpdateViewMapper.toReadUpdateView(ticketId, latest);
                        throw new ConflictException("已读位点已变化, 请刷新后重试");
                    }
                    return TicketReadUpdateViewMapper.toReadUpdateView(ticketId, participant);
                },
                result -> String.valueOf(result.lastReadMessageId())
        );
    }

    /**
     * 分页查询当前用户可见的工单状态日志
     *
     * @param userId     当前用户 ID
     * @param ticketNo   工单编号
     * @param pageQuery  分页参数
     * @return 状态日志分页结果
     */
    @Override
    public @NotNull PageResult<UserTicketStatusLogView> listMyTicketStatusLogs(@NotNull Long userId,
                                                                               @NotNull TicketNo ticketNo,
                                                                               @NotNull PageQuery pageQuery) {
        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        return userTicketRepository.pageTicketStatusLogs(requirePersistedTicketId(ticket), pageQuery);
    }

    /**
     * 查询当前用户工单关联的补发物流列表
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @return 物流摘要列表
     */
    @Override
    public @NotNull List<UserTicketShipmentSummaryView> listMyTicketReshipShipments(@NotNull Long userId,
                                                                                    @NotNull TicketNo ticketNo) {
        CustomerServiceTicket ticket = requireUserTicket(userId, ticketNo);
        return userTicketRepository.listTicketReshipShipments(requirePersistedTicketId(ticket));
    }

    /**
     * 创建当前用户的 WebSocket 会话签发结果
     *
     * @param userId          当前用户 ID
     * @param command         会话创建命令
     * @param idempotencyKey  请求幂等键
     * @return 会话签发结果
     */
    @Override
    public @NotNull TicketWsSessionIssueView createMyWsSession(@NotNull Long userId,
                                                               @NotNull TicketWsSessionCreateCommand command,
                                                               @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = idempotencyPort().registerActionOrGet(
                IDEMPOTENCY_SCENE_WS_SESSION_CREATE,
                userId,
                "user",
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded()) {
            String existedToken = tokenStatus.ticketNo();
            if (existedToken != null && !existedToken.isBlank())
                return ticketWsTokenIssuer.buildSessionIssueView(
                        existedToken,
                        DEFAULT_USER_WS_URL,
                        WS_TOKEN_TTL_SECONDS,
                        WS_HEARTBEAT_INTERVAL_SECONDS,
                        WS_RESUME_TTL_SECONDS
                );
        }

        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的会话签发请求正在处理中");

        if (!command.ticketNos().isEmpty()) {
            List<Long> ownedTicketIds = userTicketRepository.listOwnedTicketIdsByNos(userId, command.ticketNos());
            require(ownedTicketIds.size() == command.ticketNos().size(), "存在无权限订阅的 ticket_no");
        }
        if (!command.ticketIds().isEmpty()) {
            List<Long> ownedTicketIds = userTicketRepository.listOwnedTicketIdsByIds(userId, command.ticketIds());
            require(ownedTicketIds.size() == command.ticketIds().size(), "存在无权限订阅的 ticket_id");
        }

        String wsToken = ticketWsTokenIssuer.issueWsToken(userId, TicketActorStrategy.USER, WS_TOKEN_TTL_SECONDS);
        markActionSucceeded(
                IDEMPOTENCY_SCENE_WS_SESSION_CREATE,
                userId,
                "user",
                idempotencyKey,
                wsToken
        );
        return ticketWsTokenIssuer.buildSessionIssueView(
                wsToken,
                DEFAULT_USER_WS_URL,
                WS_TOKEN_TTL_SECONDS,
                WS_HEARTBEAT_INTERVAL_SECONDS,
                WS_RESUME_TTL_SECONDS
        );
    }

    /**
     * 查询并校验工单归属
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @return 工单聚合
     */
    private @NotNull CustomerServiceTicket requireUserTicket(@NotNull Long userId,
                                                             @NotNull TicketNo ticketNo) {
        return userTicketRepository.findByUserAndTicketNo(userId, ticketNo)
                .orElseThrow(() -> new NotFoundException("工单不存在"));
    }

    /**
     * 校验工单已持久化并返回主键
     *
     * @param ticket 工单聚合
     * @return 工单主键
     */
    private @NotNull Long requirePersistedTicketId(@NotNull CustomerServiceTicket ticket) {
        Long ticketId = ticket.getId();
        requireNotNull(ticketId, "ticketId 不能为空");
        require(ticketId > 0, "ticketId 必须大于 0");
        return ticketId;
    }

    /**
     * 构造来源引用标识
     *
     * @param prefix  来源前缀
     * @param tail    来源尾部
     * @return 截断后的来源引用标识
     */
    private String buildSourceRef(@NotNull String prefix, @NotNull String tail) {
        int maxLength = 128;
        if (prefix.length() >= maxLength)
            return prefix.substring(0, maxLength);

        int remain = maxLength - prefix.length();
        String normalizedTail = tail.length() <= remain ? tail : tail.substring(0, remain);
        return prefix + normalizedTail;
    }
}
