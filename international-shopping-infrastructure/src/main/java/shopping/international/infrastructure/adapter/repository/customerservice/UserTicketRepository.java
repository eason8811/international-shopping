package shopping.international.infrastructure.adapter.repository.customerservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.customerservice.IUserTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.*;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;
import shopping.international.infrastructure.dao.customerservice.CsTicketMapper;
import shopping.international.infrastructure.dao.customerservice.CsTicketParticipantMapper;
import shopping.international.infrastructure.dao.customerservice.CsTicketStatusLogMapper;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketParticipantPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketStatusLogPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketDetailPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketSummaryPO;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 用户侧工单仓储实现, 基于 MyBatis 和 MySQL
 */
@Repository
@RequiredArgsConstructor
public class UserTicketRepository implements IUserTicketRepository {

    /**
     * 字符串列表 JSON 类型引用
     */
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    /**
     * 开放状态集合, 用于命中 uk_ticket_open_dedupe 语义
     */
    private static final List<String> OPEN_STATUS_LIST = List.of(
            TicketStatus.OPEN.name(),
            TicketStatus.IN_PROGRESS.name(),
            TicketStatus.AWAITING_USER.name(),
            TicketStatus.AWAITING_CARRIER.name(),
            TicketStatus.ON_HOLD.name()
    );

    /**
     * 工单主表 Mapper
     */
    private final CsTicketMapper csTicketMapper;
    /**
     * 工单参与方 Mapper
     */
    private final CsTicketParticipantMapper csTicketParticipantMapper;
    /**
     * 工单状态日志 Mapper
     */
    private final CsTicketStatusLogMapper csTicketStatusLogMapper;
    /**
     * JSON 序列化和反序列化工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 分页查询用户工单摘要
     *
     * @param userId       用户 ID
     * @param pageQuery    分页参数
     * @param status       状态筛选
     * @param issueType    问题类型筛选
     * @param orderNo      订单号筛选
     * @param shipmentNo   物流单号筛选
     * @param createdFrom  创建时间起始
     * @param createdTo    创建时间结束
     * @return 工单摘要分页结果
     */
    @Override
    public @NotNull PageResult<UserTicketSummaryView> pageUserTicketSummaries(@NotNull Long userId,
                                                                              @NotNull PageQuery pageQuery,
                                                                              @Nullable TicketStatus status,
                                                                              @Nullable TicketIssueType issueType,
                                                                              @Nullable String orderNo,
                                                                              @Nullable String shipmentNo,
                                                                              @Nullable LocalDateTime createdFrom,
                                                                              @Nullable LocalDateTime createdTo) {
        pageQuery.validate();

        String statusValue = status == null ? null : status.name();
        String issueTypeValue = issueType == null ? null : issueType.name();
        List<CsUserTicketSummaryPO> rowList = csTicketMapper.pageUserTicketSummaries(
                userId,
                statusValue,
                issueTypeValue,
                orderNo,
                shipmentNo,
                createdFrom,
                createdTo,
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = csTicketMapper.countUserTicketSummaries(
                userId,
                statusValue,
                issueTypeValue,
                orderNo,
                shipmentNo,
                createdFrom,
                createdTo
        );

        if (rowList == null || rowList.isEmpty())
            return PageResult.<UserTicketSummaryView>builder()
                    .items(List.of())
                    .total(total)
                    .build();

        List<UserTicketSummaryView> itemList = rowList.stream()
                .map(this::toUserTicketSummaryView)
                .toList();
        return PageResult.<UserTicketSummaryView>builder()
                .items(itemList)
                .total(total)
                .build();
    }

    /**
     * 按用户和工单编号查询工单详情视图
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 工单详情视图
     */
    @Override
    public @NotNull Optional<UserTicketDetailView> findUserTicketDetail(@NotNull Long userId,
                                                                        @NotNull TicketNo ticketNo) {
        CsUserTicketDetailPO row = csTicketMapper.selectUserTicketDetail(userId, ticketNo.getValue());
        if (row == null)
            return Optional.empty();
        return Optional.of(toUserTicketDetailView(row));
    }

    /**
     * 按用户和工单编号查询工单聚合
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 工单聚合
     */
    @Override
    public @NotNull Optional<CustomerServiceTicket> findByUserAndTicketNo(@NotNull Long userId,
                                                                          @NotNull TicketNo ticketNo) {
        LambdaQueryWrapper<CsTicketPO> queryWrapper = new LambdaQueryWrapper<CsTicketPO>()
                .eq(CsTicketPO::getUserId, userId)
                .eq(CsTicketPO::getTicketNo, ticketNo.getValue())
                .last("LIMIT 1");
        CsTicketPO row = csTicketMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(toTicketAggregate(row));
    }

    /**
     * 按去重维度查询进行中的工单
     *
     * @param userId      用户 ID
     * @param orderId     订单 ID
     * @param shipmentId  物流单 ID
     * @param issueType   问题类型
     * @return 进行中的工单
     */
    @Override
    public @NotNull Optional<CustomerServiceTicket> findOpenTicketByDedupe(@NotNull Long userId,
                                                                           @NotNull Long orderId,
                                                                           @Nullable Long shipmentId,
                                                                           @NotNull TicketIssueType issueType) {
        LambdaQueryWrapper<CsTicketPO> queryWrapper = new LambdaQueryWrapper<CsTicketPO>()
                .eq(CsTicketPO::getUserId, userId)
                .eq(CsTicketPO::getOrderId, orderId)
                .eq(CsTicketPO::getIssueType, issueType.name())
                .in(CsTicketPO::getStatus, OPEN_STATUS_LIST)
                .orderByDesc(CsTicketPO::getId)
                .last("LIMIT 1");
        if (shipmentId == null)
            queryWrapper.isNull(CsTicketPO::getShipmentId);
        else
            queryWrapper.eq(CsTicketPO::getShipmentId, shipmentId);

        CsTicketPO row = csTicketMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(toTicketAggregate(row));
    }

    /**
     * 保存新建工单, 同时写入默认参与方和初始化状态日志
     *
     * @param ticket        待保存工单聚合
     * @param ownerUserId   发起用户 ID
     * @param sourceRef     来源引用标识
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull UserTicketCreateResult saveNewTicket(@NotNull CustomerServiceTicket ticket,
                                                         @NotNull Long ownerUserId,
                                                         @Nullable String sourceRef) {
        CsTicketPO insertRow = toInsertTicketPO(ticket);
        try {
            int affectedRowCount = csTicketMapper.insert(insertRow);
            require(affectedRowCount == 1, "创建工单失败");
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("同一订单和问题类型下已存在进行中的工单");
        }

        Long ticketId = insertRow.getId();
        if (ticketId == null)
            throw new AppException("创建工单后未返回主键");

        CsTicketParticipantPO participantRow = CsTicketParticipantPO.builder()
                .ticketId(ticketId)
                .participantType(TicketParticipantType.USER.name())
                .participantUserId(ownerUserId)
                .role(TicketParticipantRole.OWNER.name())
                .build();
        int participantAffected = csTicketParticipantMapper.insert(participantRow);
        require(participantAffected == 1, "写入工单默认参与方失败");

        CsTicketStatusLogPO statusLogRow = CsTicketStatusLogPO.builder()
                .ticketId(ticketId)
                .fromStatus(null)
                .toStatus(TicketStatus.OPEN.name())
                .actorType(TicketActorType.USER.name())
                .actorUserId(ownerUserId)
                .sourceRef(sourceRef)
                .note("USER_CREATE")
                .build();
        int statusLogAffected = csTicketStatusLogMapper.insert(statusLogRow);
        require(statusLogAffected == 1, "写入工单初始状态日志失败");

        CsTicketPO persistedRow = csTicketMapper.selectById(ticketId);
        if (persistedRow == null)
            throw new AppException("创建工单后回读失败");
        return new UserTicketCreateResult(
                persistedRow.getId(),
                persistedRow.getTicketNo(),
                TicketStatus.fromValue(requireColumn(persistedRow.getStatus(), "status")),
                requireColumn(persistedRow.getCreatedAt(), "createdAt")
        );
    }

    /**
     * 按用户和工单编号查询创建结果
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 创建结果
     */
    @Override
    public @NotNull Optional<UserTicketCreateResult> findUserTicketCreateResult(@NotNull Long userId,
                                                                                @NotNull TicketNo ticketNo) {
        LambdaQueryWrapper<CsTicketPO> queryWrapper = new LambdaQueryWrapper<CsTicketPO>()
                .eq(CsTicketPO::getUserId, userId)
                .eq(CsTicketPO::getTicketNo, ticketNo.getValue())
                .last("LIMIT 1");
        CsTicketPO row = csTicketMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(new UserTicketCreateResult(
                row.getId(),
                row.getTicketNo(),
                TicketStatus.fromValue(requireColumn(row.getStatus(), "status")),
                requireColumn(row.getCreatedAt(), "createdAt")
        ));
    }

    /**
     * 基于状态 CAS 更新工单状态, 并写入状态日志
     *
     * @param userId               用户 ID
     * @param ticket               已完成状态推进的工单聚合
     * @param expectedFromStatus   期望旧状态
     * @param statusLog            状态日志实体
     * @return 更新是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTicketStatusWithCasAndAppendLog(@NotNull Long userId,
                                                         @NotNull CustomerServiceTicket ticket,
                                                         @NotNull TicketStatus expectedFromStatus,
                                                         @NotNull TicketStatusLog statusLog) {
        Long ticketId = ticket.getId();
        if (ticketId == null)
            throw new AppException("工单未持久化, 无法更新状态");

        int affectedRowCount = csTicketMapper.updateStatusWithCas(
                ticketId,
                userId,
                expectedFromStatus.name(),
                ticket.getStatus().name(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticket.getUpdatedAt()
        );
        if (affectedRowCount <= 0)
            return false;

        CsTicketStatusLogPO statusLogRow = toStatusLogPO(statusLog);
        int statusLogAffected = csTicketStatusLogMapper.insert(statusLogRow);
        require(statusLogAffected == 1, "写入工单状态日志失败");
        return true;
    }

    /**
     * 持久化行转换为用户工单摘要视图
     *
     * @param row 持久化行
     * @return 用户工单摘要视图
     */
    private @NotNull UserTicketSummaryView toUserTicketSummaryView(@NotNull CsUserTicketSummaryPO row) {
        String ticketNo = requireColumn(row.getTicketNo(), "ticketNo");
        String issueType = requireColumn(row.getIssueType(), "issueType");
        String status = requireColumn(row.getStatus(), "status");
        String title = requireColumn(row.getTitle(), "title");
        Long orderId = requireColumn(row.getOrderId(), "orderId");
        String orderNo = requireColumn(row.getOrderNo(), "orderNo");
        String orderStatus = requireColumn(row.getOrderStatus(), "orderStatus");
        String payCurrency = normalizeCurrency(row.getPayCurrency());
        String orderCover = row.getOrderCover() == null ? "" : row.getOrderCover();

        return new UserTicketSummaryView(
                ticketNo,
                TicketIssueType.fromValue(issueType),
                TicketStatus.fromValue(status),
                title,
                orderId,
                orderNo,
                OrderStatus.valueOf(orderStatus),
                row.getPayAmount() == null ? 0L : row.getPayAmount(),
                payCurrency,
                orderCover,
                row.getShipmentId(),
                row.getShipmentStatus() == null ? null : ShipmentStatus.valueOf(row.getShipmentStatus()),
                row.getShipmentStatusLogSnapshot(),
                row.getAssignedToUserId(),
                row.getLastMessageAt(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt")
        );
    }

    /**
     * 持久化行转换为用户工单详情视图
     *
     * @param row 持久化行
     * @return 用户工单详情视图
     */
    private @NotNull UserTicketDetailView toUserTicketDetailView(@NotNull CsUserTicketDetailPO row) {
        String ticketNo = requireColumn(row.getTicketNo(), "ticketNo");
        String issueType = requireColumn(row.getIssueType(), "issueType");
        String status = requireColumn(row.getStatus(), "status");
        String title = requireColumn(row.getTitle(), "title");
        Long orderId = requireColumn(row.getOrderId(), "orderId");
        String orderNo = requireColumn(row.getOrderNo(), "orderNo");
        String orderStatus = requireColumn(row.getOrderStatus(), "orderStatus");
        String payCurrency = normalizeCurrency(row.getPayCurrency());
        String orderCover = row.getOrderCover() == null ? "" : row.getOrderCover();

        return new UserTicketDetailView(
                requireColumn(row.getTicketId(), "ticketId"),
                ticketNo,
                TicketIssueType.fromValue(issueType),
                TicketStatus.fromValue(status),
                title,
                orderId,
                orderNo,
                OrderStatus.valueOf(orderStatus),
                row.getPayAmount() == null ? 0L : row.getPayAmount(),
                payCurrency,
                orderCover,
                row.getShipmentId(),
                row.getShipmentStatus() == null ? null : ShipmentStatus.valueOf(row.getShipmentStatus()),
                row.getShipmentStatusLogSnapshot(),
                row.getAssignedToUserId(),
                row.getLastMessageAt(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt"),
                row.getDescription(),
                parseStringList(row.getAttachments(), "attachments"),
                parseStringList(row.getEvidence(), "evidence"),
                parseStringList(row.getTags(), "tags"),
                row.getRequestedRefundAmount(),
                row.getCurrency(),
                row.getResolvedAt(),
                row.getClosedAt(),
                row.getSlaDueAt()
        );
    }

    /**
     * 持久化行转换为工单聚合
     *
     * @param row 持久化行
     * @return 工单聚合
     */
    private @NotNull CustomerServiceTicket toTicketAggregate(@NotNull CsTicketPO row) {
        return CustomerServiceTicket.reconstitute(
                row.getId(),
                TicketNo.of(requireColumn(row.getTicketNo(), "ticketNo")),
                requireColumn(row.getUserId(), "userId"),
                row.getOrderId(),
                row.getOrderItemId(),
                row.getShipmentId(),
                TicketIssueType.fromValue(requireColumn(row.getIssueType(), "issueType")),
                TicketStatus.fromValue(requireColumn(row.getStatus(), "status")),
                TicketPriority.fromValue(requireColumn(row.getPriority(), "priority")),
                TicketChannel.fromValue(requireColumn(row.getChannel(), "channel")),
                requireColumn(row.getTitle(), "title"),
                row.getDescription(),
                parseStringList(row.getAttachments(), "attachments"),
                parseStringList(row.getEvidence(), "evidence"),
                parseStringList(row.getTags(), "tags"),
                row.getRequestedRefundAmount(),
                row.getCurrency(),
                row.getClaimExternalId(),
                row.getAssignedToUserId(),
                row.getAssignedAt(),
                row.getLastMessageAt(),
                row.getSlaDueAt(),
                row.getResolvedAt(),
                row.getClosedAt(),
                row.getStatusChangedAt(),
                row.getSourceRef(),
                row.getExtra(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt"),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    /**
     * 领域聚合转换为工单持久化行
     *
     * @param ticket 工单聚合
     * @return 工单持久化行
     */
    private @NotNull CsTicketPO toInsertTicketPO(@NotNull CustomerServiceTicket ticket) {
        return CsTicketPO.builder()
                .ticketNo(ticket.getTicketNo().getValue())
                .userId(ticket.getUserId())
                .orderId(ticket.getOrderId())
                .orderItemId(ticket.getOrderItemId())
                .shipmentId(ticket.getShipmentId())
                .issueType(ticket.getIssueType().name())
                .status(ticket.getStatus().name())
                .priority(ticket.getPriority().name())
                .channel(ticket.getChannel().name())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .attachments(toJsonArray(ticket.getAttachments(), "attachments"))
                .evidence(toJsonArray(ticket.getEvidence(), "evidence"))
                .tags(toJsonArray(ticket.getTags(), "tags"))
                .requestedRefundAmount(ticket.getRequestedRefundAmount())
                .currency(ticket.getCurrency())
                .claimExternalId(ticket.getClaimExternalId())
                .assignedToUserId(ticket.getAssignedToUserId())
                .assignedAt(ticket.getAssignedAt())
                .lastMessageAt(ticket.getLastMessageAt())
                .slaDueAt(ticket.getSlaDueAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .statusChangedAt(ticket.getStatusChangedAt())
                .sourceRef(ticket.getSourceRef())
                .extra(ticket.getExtra())
                .build();
    }

    /**
     * 状态日志实体转换为状态日志持久化行
     *
     * @param statusLog 状态日志实体
     * @return 状态日志持久化行
     */
    private @NotNull CsTicketStatusLogPO toStatusLogPO(@NotNull TicketStatusLog statusLog) {
        return CsTicketStatusLogPO.builder()
                .ticketId(statusLog.getTicketId())
                .fromStatus(statusLog.getFromStatus() == null ? null : statusLog.getFromStatus().name())
                .toStatus(statusLog.getToStatus().name())
                .actorType(statusLog.getActorType().name())
                .actorUserId(statusLog.getActorUserId())
                .sourceRef(statusLog.getSourceRef())
                .note(statusLog.getNote())
                .createdAt(statusLog.getCreatedAt())
                .build();
    }

    /**
     * 字符串列表序列化为 JSON 数组, 空列表返回 null
     *
     * @param valueList 字符串列表
     * @param fieldName 字段名
     * @return JSON 字符串
     */
    private @Nullable String toJsonArray(@Nullable List<String> valueList, @NotNull String fieldName) {
        if (valueList == null || valueList.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(valueList);
        } catch (JsonProcessingException exception) {
            throw new AppException("序列化 " + fieldName + " 失败");
        }
    }

    /**
     * JSON 数组反序列化为字符串列表, 空 JSON 返回空列表
     *
     * @param jsonValue JSON 字符串
     * @param fieldName 字段名
     * @return 字符串列表
     */
    private @NotNull List<String> parseStringList(@Nullable String jsonValue, @NotNull String fieldName) {
        if (jsonValue == null || jsonValue.isBlank())
            return List.of();
        try {
            List<String> parsedList = objectMapper.readValue(jsonValue, STRING_LIST_TYPE);
            if (parsedList == null || parsedList.isEmpty())
                return List.of();
            return parsedList.stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::strip)
                    .toList();
        } catch (Exception exception) {
            throw new AppException("解析 " + fieldName + " 失败");
        }
    }

    /**
     * 校验列值非空, 返回列值
     *
     * @param value      列值
     * @param columnName 列名
     * @return 非空列值
     */
    private <T> @NotNull T requireColumn(@Nullable T value, @NotNull String columnName) {
        require(value != null, columnName + " 不能为空");
        return value;
    }
}
