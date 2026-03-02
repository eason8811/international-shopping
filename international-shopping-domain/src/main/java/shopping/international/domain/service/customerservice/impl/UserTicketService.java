package shopping.international.domain.service.customerservice.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;
import shopping.international.domain.adapter.repository.customerservice.IUserTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketChannel;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.TicketCreateCommand;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;
import shopping.international.domain.service.customerservice.IUserTicketService;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 用户侧工单领域服务实现
 */
@Service
@RequiredArgsConstructor
public class UserTicketService implements IUserTicketService {

    /**
     * 用户侧工单仓储
     */
    private final IUserTicketRepository userTicketRepository;
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
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerCreateOrGet(
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
        ticketIdempotencyPort.markCreateSucceeded(userId, idempotencyKey, created.ticketNo(), IDEMPOTENCY_SUCCESS_TTL);
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
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerCloseOrGet(
                userId,
                ticketNo.getValue(),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );

        if (tokenStatus.isSucceeded())
            return getMyTicketDetail(userId, ticketNo);

        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的关闭请求正在处理中");

        CustomerServiceTicket ticket = userTicketRepository.findByUserAndTicketNo(userId, ticketNo)
                .orElseThrow(() -> new NotFoundException("工单不存在"));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            UserTicketDetailView detailView = getMyTicketDetail(userId, ticketNo);
            ticketIdempotencyPort.markCloseSucceeded(userId, ticketNo.getValue(), idempotencyKey, IDEMPOTENCY_SUCCESS_TTL);
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
            CustomerServiceTicket latest = userTicketRepository.findByUserAndTicketNo(userId, ticketNo)
                    .orElseThrow(() -> new NotFoundException("工单不存在"));
            if (latest.getStatus() == TicketStatus.CLOSED) {
                UserTicketDetailView detailView = getMyTicketDetail(userId, ticketNo);
                ticketIdempotencyPort.markCloseSucceeded(userId, ticketNo.getValue(), idempotencyKey, IDEMPOTENCY_SUCCESS_TTL);
                return detailView;
            }
            throw new ConflictException("工单状态已变化, 请刷新后重试");
        }

        UserTicketDetailView detailView = getMyTicketDetail(userId, ticketNo);
        ticketIdempotencyPort.markCloseSucceeded(userId, ticketNo.getValue(), idempotencyKey, IDEMPOTENCY_SUCCESS_TTL);
        return detailView;
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
