package shopping.international.domain.service.customerservice.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;
import shopping.international.domain.adapter.repository.customerservice.IAdminTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketAssignmentActionType;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketPageCriteria;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;
import shopping.international.domain.service.customerservice.IAdminTicketService;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.Duration;
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
        TicketParticipant participant = adminTicketRepository.findActiveParticipant(
                        persistedTicketId,
                        TicketParticipantType.AGENT,
                        actorUserId
                )
                .orElseThrow(() -> new ConflictException("当前坐席不是工单活跃参与方"));
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
     * @param detailView         最新工单详情
     * @param toAssigneeUserId   目标指派坐席用户 ID
     * @param actionType         指派动作
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
