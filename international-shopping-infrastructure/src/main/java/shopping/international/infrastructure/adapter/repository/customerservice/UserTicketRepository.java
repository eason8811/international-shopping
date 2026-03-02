package shopping.international.infrastructure.adapter.repository.customerservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.customerservice.IAdminTicketRepository;
import shopping.international.domain.adapter.repository.customerservice.IUserTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.*;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketPageCriteria;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;
import shopping.international.domain.model.vo.customerservice.TicketMessageNo;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketMessageView;
import shopping.international.domain.model.vo.customerservice.UserTicketShipmentSummaryView;
import shopping.international.domain.model.vo.customerservice.UserTicketStatusLogView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;
import shopping.international.infrastructure.dao.customerservice.CsTicketMapper;
import shopping.international.infrastructure.dao.customerservice.CsTicketAssignmentLogMapper;
import shopping.international.infrastructure.dao.customerservice.CsTicketMessageMapper;
import shopping.international.infrastructure.dao.customerservice.CsTicketParticipantMapper;
import shopping.international.infrastructure.dao.customerservice.CsTicketStatusLogMapper;
import shopping.international.infrastructure.dao.customerservice.po.CsAdminTicketDetailPO;
import shopping.international.infrastructure.dao.customerservice.po.CsAdminTicketSummaryPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketAssignmentLogPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketMessagePO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketParticipantPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketStatusLogPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketDetailPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketShipmentSummaryPO;
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
public class UserTicketRepository implements IUserTicketRepository, IAdminTicketRepository {

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
     * 工单消息 Mapper
     */
    private final CsTicketMessageMapper csTicketMessageMapper;
    /**
     * 工单状态日志 Mapper
     */
    private final CsTicketStatusLogMapper csTicketStatusLogMapper;
    /**
     * 工单指派日志 Mapper
     */
    private final CsTicketAssignmentLogMapper csTicketAssignmentLogMapper;
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
     * 查询工单消息列表
     *
     * @param ticketId   工单 ID
     * @param beforeId   向前锚点
     * @param afterId    向后锚点
     * @param ascOrder   是否升序
     * @param size       返回条数
     * @return 消息列表
     */
    @Override
    public @NotNull List<UserTicketMessageView> listTicketMessages(@NotNull Long ticketId,
                                                                   @Nullable Long beforeId,
                                                                   @Nullable Long afterId,
                                                                   boolean ascOrder,
                                                                   int size) {
        LambdaQueryWrapper<CsTicketMessagePO> queryWrapper = new LambdaQueryWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getTicketId, ticketId);
        if (beforeId != null)
            queryWrapper.lt(CsTicketMessagePO::getId, beforeId);
        if (afterId != null)
            queryWrapper.gt(CsTicketMessagePO::getId, afterId);
        queryWrapper.orderBy(true, ascOrder, CsTicketMessagePO::getId)
                .last("LIMIT " + size);

        List<CsTicketMessagePO> rowList = csTicketMessageMapper.selectList(queryWrapper);
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .map(this::toUserTicketMessageView)
                .toList();
    }

    /**
     * 按消息编号查询消息实体
     *
     * @param ticketId   工单 ID
     * @param messageNo  消息编号
     * @return 消息实体
     */
    @Override
    public @NotNull Optional<TicketMessage> findTicketMessageByNo(@NotNull Long ticketId,
                                                                  @NotNull TicketMessageNo messageNo) {
        LambdaQueryWrapper<CsTicketMessagePO> queryWrapper = new LambdaQueryWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getTicketId, ticketId)
                .eq(CsTicketMessagePO::getMessageNo, messageNo.getValue())
                .last("LIMIT 1");
        CsTicketMessagePO row = csTicketMessageMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(toTicketMessageEntity(row));
    }

    /**
     * 按消息编号查询消息视图
     *
     * @param ticketId   工单 ID
     * @param messageNo  消息编号
     * @return 消息视图
     */
    @Override
    public @NotNull Optional<UserTicketMessageView> findTicketMessageViewByNo(@NotNull Long ticketId,
                                                                              @NotNull TicketMessageNo messageNo) {
        LambdaQueryWrapper<CsTicketMessagePO> queryWrapper = new LambdaQueryWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getTicketId, ticketId)
                .eq(CsTicketMessagePO::getMessageNo, messageNo.getValue())
                .last("LIMIT 1");
        CsTicketMessagePO row = csTicketMessageMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(toUserTicketMessageView(row));
    }

    /**
     * 按客户端消息幂等键查询消息视图
     *
     * @param ticketId          工单 ID
     * @param senderType        发送方类型
     * @param senderUserId      发送方用户 ID
     * @param clientMessageId   客户端消息幂等键
     * @return 消息视图
     */
    @Override
    public @NotNull Optional<UserTicketMessageView> findMessageViewByClientMessageId(@NotNull Long ticketId,
                                                                                     @NotNull TicketParticipantType senderType,
                                                                                     @Nullable Long senderUserId,
                                                                                     @NotNull String clientMessageId) {
        LambdaQueryWrapper<CsTicketMessagePO> queryWrapper = new LambdaQueryWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getTicketId, ticketId)
                .eq(CsTicketMessagePO::getSenderType, senderType.name())
                .eq(CsTicketMessagePO::getClientMessageId, clientMessageId)
                .last("LIMIT 1");
        if (senderUserId == null)
            queryWrapper.isNull(CsTicketMessagePO::getSenderUserId);
        else
            queryWrapper.eq(CsTicketMessagePO::getSenderUserId, senderUserId);

        CsTicketMessagePO row = csTicketMessageMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(toUserTicketMessageView(row));
    }

    /**
     * 保存工单消息并更新工单最近消息时间
     *
     * @param userId    用户 ID
     * @param ticket    工单聚合
     * @param message   消息实体
     * @return 落库后的消息视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull UserTicketMessageView saveTicketMessageAndTouchTicket(@NotNull Long userId,
                                                                          @NotNull CustomerServiceTicket ticket,
                                                                          @NotNull TicketMessage message) {
        Long ticketId = ticket.getId();
        if (ticketId == null)
            throw new AppException("工单未持久化, 无法写入消息");

        CsTicketMessagePO insertRow = toTicketMessagePO(message);
        try {
            int affectedRowCount = csTicketMessageMapper.insert(insertRow);
            require(affectedRowCount == 1, "写入工单消息失败");
        } catch (DuplicateKeyException exception) {
            String clientMessageId = requireColumn(message.getClientMessageId(), "clientMessageId");
            return findMessageViewByClientMessageId(ticketId, message.getSenderType(), message.getSenderUserId(), clientMessageId)
                    .orElseThrow(() -> new AppException("命中消息去重键, 但未找到已存在消息"));
        }

        int touchAffected = csTicketMapper.touchLastMessageAt(ticketId, userId, message.getSentAt());
        require(touchAffected == 1, "更新工单最近消息时间失败");

        Long messageId = insertRow.getId();
        if (messageId == null)
            throw new AppException("写入工单消息后未返回主键");

        CsTicketMessagePO persistedRow = csTicketMessageMapper.selectById(messageId);
        if (persistedRow == null)
            throw new AppException("写入工单消息后回读失败");
        return toUserTicketMessageView(persistedRow);
    }

    /**
     * 基于 CAS 条件更新消息内容
     *
     * @param ticketId   工单 ID
     * @param messageId  消息 ID
     * @param content    新内容
     * @param editedAt   编辑时间
     * @param updatedAt  更新时间
     * @return 更新是否成功
     */
    @Override
    public boolean updateTicketMessageContentWithCas(@NotNull Long ticketId,
                                                     @NotNull Long messageId,
                                                     @NotNull String content,
                                                     @NotNull LocalDateTime editedAt,
                                                     @NotNull LocalDateTime updatedAt) {
        CsTicketMessagePO updateRow = CsTicketMessagePO.builder()
                .content(content)
                .editedAt(editedAt)
                .updatedAt(updatedAt)
                .build();
        LambdaUpdateWrapper<CsTicketMessagePO> updateWrapper = new LambdaUpdateWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getId, messageId)
                .eq(CsTicketMessagePO::getTicketId, ticketId)
                .isNull(CsTicketMessagePO::getRecalledAt);
        int affectedRowCount = csTicketMessageMapper.update(updateRow, updateWrapper);
        return affectedRowCount > 0;
    }

    /**
     * 基于 CAS 条件撤回消息
     *
     * @param ticketId         工单 ID
     * @param messageId        消息 ID
     * @param recalledContent  撤回占位内容
     * @param recalledAt       撤回时间
     * @param updatedAt        更新时间
     * @return 更新是否成功
     */
    @Override
    public boolean recallTicketMessageWithCas(@NotNull Long ticketId,
                                              @NotNull Long messageId,
                                              @NotNull String recalledContent,
                                              @NotNull LocalDateTime recalledAt,
                                              @NotNull LocalDateTime updatedAt) {
        CsTicketMessagePO updateRow = CsTicketMessagePO.builder()
                .content(recalledContent)
                .attachments("[]")
                .recalledAt(recalledAt)
                .updatedAt(updatedAt)
                .build();
        LambdaUpdateWrapper<CsTicketMessagePO> updateWrapper = new LambdaUpdateWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getId, messageId)
                .eq(CsTicketMessagePO::getTicketId, ticketId)
                .isNull(CsTicketMessagePO::getRecalledAt);
        int affectedRowCount = csTicketMessageMapper.update(updateRow, updateWrapper);
        return affectedRowCount > 0;
    }

    /**
     * 查询活跃参与方
     *
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @return 参与方实体
     */
    @Override
    public @NotNull Optional<TicketParticipant> findActiveParticipant(@NotNull Long ticketId,
                                                                      @NotNull TicketParticipantType participantType,
                                                                      @Nullable Long participantUserId) {
        LambdaQueryWrapper<CsTicketParticipantPO> queryWrapper = new LambdaQueryWrapper<CsTicketParticipantPO>()
                .eq(CsTicketParticipantPO::getTicketId, ticketId)
                .eq(CsTicketParticipantPO::getParticipantType, participantType.name())
                .isNull(CsTicketParticipantPO::getLeftAt)
                .last("LIMIT 1");
        if (participantUserId == null)
            queryWrapper.isNull(CsTicketParticipantPO::getParticipantUserId);
        else
            queryWrapper.eq(CsTicketParticipantPO::getParticipantUserId, participantUserId);

        CsTicketParticipantPO row = csTicketParticipantMapper.selectOne(queryWrapper);
        if (row == null)
            return Optional.empty();
        return Optional.of(toTicketParticipantEntity(row));
    }

    /**
     * 校验消息 ID 是否属于工单
     *
     * @param ticketId   工单 ID
     * @param messageId  消息 ID
     * @return 是否属于当前工单
     */
    @Override
    public boolean existsMessageInTicket(@NotNull Long ticketId,
                                         @NotNull Long messageId) {
        LambdaQueryWrapper<CsTicketMessagePO> queryWrapper = new LambdaQueryWrapper<CsTicketMessagePO>()
                .eq(CsTicketMessagePO::getTicketId, ticketId)
                .eq(CsTicketMessagePO::getId, messageId);
        return csTicketMessageMapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 基于 CAS 条件更新参与方已读位点
     *
     * @param participantId      参与方 ID
     * @param ticketId           工单 ID
     * @param lastReadMessageId  最后已读消息 ID
     * @param lastReadAt         最后已读时间
     * @param updatedAt          更新时间
     * @return 更新是否成功
     */
    @Override
    public boolean updateParticipantReadWithCas(@NotNull Long participantId,
                                                @NotNull Long ticketId,
                                                @NotNull Long lastReadMessageId,
                                                @NotNull LocalDateTime lastReadAt,
                                                @NotNull LocalDateTime updatedAt) {
        CsTicketParticipantPO updateRow = CsTicketParticipantPO.builder()
                .lastReadMessageId(lastReadMessageId)
                .lastReadAt(lastReadAt)
                .updatedAt(updatedAt)
                .build();
        LambdaUpdateWrapper<CsTicketParticipantPO> updateWrapper = new LambdaUpdateWrapper<CsTicketParticipantPO>()
                .eq(CsTicketParticipantPO::getId, participantId)
                .eq(CsTicketParticipantPO::getTicketId, ticketId)
                .and(wrapper -> wrapper.isNull(CsTicketParticipantPO::getLastReadMessageId)
                        .or()
                        .le(CsTicketParticipantPO::getLastReadMessageId, lastReadMessageId));
        int affectedRowCount = csTicketParticipantMapper.update(updateRow, updateWrapper);
        return affectedRowCount > 0;
    }

    /**
     * 分页查询工单状态日志
     *
     * @param ticketId    工单 ID
     * @param pageQuery   分页参数
     * @return 状态日志分页结果
     */
    @Override
    public @NotNull PageResult<UserTicketStatusLogView> pageTicketStatusLogs(@NotNull Long ticketId,
                                                                             @NotNull PageQuery pageQuery) {
        pageQuery.validate();
        LambdaQueryWrapper<CsTicketStatusLogPO> queryWrapper = new LambdaQueryWrapper<CsTicketStatusLogPO>()
                .eq(CsTicketStatusLogPO::getTicketId, ticketId)
                .orderByDesc(CsTicketStatusLogPO::getCreatedAt, CsTicketStatusLogPO::getId)
                .last("LIMIT " + pageQuery.limit() + " OFFSET " + pageQuery.offset());
        List<CsTicketStatusLogPO> rowList = csTicketStatusLogMapper.selectList(queryWrapper);

        LambdaQueryWrapper<CsTicketStatusLogPO> countWrapper = new LambdaQueryWrapper<CsTicketStatusLogPO>()
                .eq(CsTicketStatusLogPO::getTicketId, ticketId);
        long total = csTicketStatusLogMapper.selectCount(countWrapper);
        if (rowList == null || rowList.isEmpty())
            return PageResult.<UserTicketStatusLogView>builder()
                    .items(List.of())
                    .total(total)
                    .build();

        List<UserTicketStatusLogView> itemList = rowList.stream()
                .map(this::toUserTicketStatusLogView)
                .toList();
        return PageResult.<UserTicketStatusLogView>builder()
                .items(itemList)
                .total(total)
                .build();
    }

    /**
     * 查询工单关联补发物流摘要
     *
     * @param ticketId 工单 ID
     * @return 物流摘要列表
     */
    @Override
    public @NotNull List<UserTicketShipmentSummaryView> listTicketReshipShipments(@NotNull Long ticketId) {
        List<CsUserTicketShipmentSummaryPO> rowList = csTicketMapper.listTicketReshipShipmentSummaries(ticketId);
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .map(this::toUserTicketShipmentSummaryView)
                .toList();
    }

    /**
     * 按工单号列表查询属于用户的工单 ID 列表
     *
     * @param userId     用户 ID
     * @param ticketNos  工单号列表
     * @return 工单 ID 列表
     */
    @Override
    public @NotNull List<Long> listOwnedTicketIdsByNos(@NotNull Long userId,
                                                       @NotNull List<String> ticketNos) {
        if (ticketNos.isEmpty())
            return List.of();
        LambdaQueryWrapper<CsTicketPO> queryWrapper = new LambdaQueryWrapper<CsTicketPO>()
                .eq(CsTicketPO::getUserId, userId)
                .in(CsTicketPO::getTicketNo, ticketNos)
                .select(CsTicketPO::getId);
        List<CsTicketPO> rowList = csTicketMapper.selectList(queryWrapper);
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .map(CsTicketPO::getId)
                .filter(item -> item != null && item > 0)
                .toList();
    }

    /**
     * 按工单 ID 列表查询属于用户的工单 ID 列表
     *
     * @param userId     用户 ID
     * @param ticketIds  工单 ID 列表
     * @return 工单 ID 列表
     */
    @Override
    public @NotNull List<Long> listOwnedTicketIdsByIds(@NotNull Long userId,
                                                       @NotNull List<Long> ticketIds) {
        if (ticketIds.isEmpty())
            return List.of();
        LambdaQueryWrapper<CsTicketPO> queryWrapper = new LambdaQueryWrapper<CsTicketPO>()
                .eq(CsTicketPO::getUserId, userId)
                .in(CsTicketPO::getId, ticketIds)
                .select(CsTicketPO::getId);
        List<CsTicketPO> rowList = csTicketMapper.selectList(queryWrapper);
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .map(CsTicketPO::getId)
                .filter(item -> item != null && item > 0)
                .toList();
    }

    /**
     * 分页查询管理侧工单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 工单摘要分页结果
     */
    @Override
    public @NotNull PageResult<AdminTicketSummaryView> pageAdminTicketSummaries(@NotNull AdminTicketPageCriteria criteria,
                                                                                @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();

        String issueType = criteria.getIssueType() == null ? null : criteria.getIssueType().name();
        String status = criteria.getStatus() == null ? null : criteria.getStatus().name();
        String priority = criteria.getPriority() == null ? null : criteria.getPriority().name();
        List<CsAdminTicketSummaryPO> rowList = csTicketMapper.pageAdminTicketSummaries(
                criteria.getTicketNo(),
                criteria.getUserId(),
                criteria.getOrderId(),
                criteria.getShipmentId(),
                issueType,
                status,
                priority,
                criteria.getAssignedToUserId(),
                criteria.getClaimExternalId(),
                criteria.getSlaDueFrom(),
                criteria.getSlaDueTo(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo(),
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = csTicketMapper.countAdminTicketSummaries(
                criteria.getTicketNo(),
                criteria.getUserId(),
                criteria.getOrderId(),
                criteria.getShipmentId(),
                issueType,
                status,
                priority,
                criteria.getAssignedToUserId(),
                criteria.getClaimExternalId(),
                criteria.getSlaDueFrom(),
                criteria.getSlaDueTo(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo()
        );

        if (rowList == null || rowList.isEmpty())
            return PageResult.<AdminTicketSummaryView>builder()
                    .items(List.of())
                    .total(total)
                    .build();

        List<AdminTicketSummaryView> itemList = rowList.stream()
                .map(this::toAdminTicketSummaryView)
                .toList();
        return PageResult.<AdminTicketSummaryView>builder()
                .items(itemList)
                .total(total)
                .build();
    }

    /**
     * 按工单 ID 查询管理侧工单详情
     *
     * @param ticketId 工单 ID
     * @return 工单详情视图
     */
    @Override
    public @NotNull Optional<AdminTicketDetailView> findAdminTicketDetail(@NotNull Long ticketId) {
        CsAdminTicketDetailPO row = csTicketMapper.selectAdminTicketDetailById(ticketId);
        if (row == null)
            return Optional.empty();
        return Optional.of(toAdminTicketDetailView(row));
    }

    /**
     * 按工单 ID 查询工单聚合
     *
     * @param ticketId 工单 ID
     * @return 工单聚合
     */
    @Override
    public @NotNull Optional<CustomerServiceTicket> findByTicketId(@NotNull Long ticketId) {
        CsTicketPO row = csTicketMapper.selectById(ticketId);
        if (row == null)
            return Optional.empty();
        return Optional.of(toTicketAggregate(row));
    }

    /**
     * 基于更新时间 CAS 更新工单元数据
     *
     * @param ticket             已完成元数据变更的工单聚合
     * @param expectedUpdatedAt  期望旧更新时间
     * @return 更新是否成功
     */
    @Override
    public boolean updateTicketMetadataWithCas(@NotNull CustomerServiceTicket ticket,
                                               @NotNull LocalDateTime expectedUpdatedAt) {
        Long ticketId = ticket.getId();
        if (ticketId == null)
            throw new AppException("工单未持久化, 无法更新元数据");

        LambdaUpdateWrapper<CsTicketPO> updateWrapper = new LambdaUpdateWrapper<CsTicketPO>()
                .eq(CsTicketPO::getId, ticketId)
                .eq(CsTicketPO::getUpdatedAt, expectedUpdatedAt)
                .set(CsTicketPO::getPriority, ticket.getPriority().name())
                .set(CsTicketPO::getTags, toJsonArray(ticket.getTags(), "tags"))
                .set(CsTicketPO::getRequestedRefundAmount, ticket.getRequestedRefundAmount())
                .set(CsTicketPO::getCurrency, ticket.getCurrency())
                .set(CsTicketPO::getClaimExternalId, ticket.getClaimExternalId())
                .set(CsTicketPO::getSlaDueAt, ticket.getSlaDueAt());
        int affectedRowCount = csTicketMapper.update(null, updateWrapper);
        return affectedRowCount > 0;
    }

    /**
     * 基于更新时间 CAS 更新工单指派信息, 并写入指派日志
     *
     * @param ticket             已完成指派变更的工单聚合
     * @param expectedUpdatedAt  期望旧更新时间
     * @param assignmentLog      指派日志实体
     * @return 更新是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTicketAssignmentWithCasAndAppendLog(@NotNull CustomerServiceTicket ticket,
                                                             @NotNull LocalDateTime expectedUpdatedAt,
                                                             @NotNull TicketAssignmentLog assignmentLog) {
        Long ticketId = ticket.getId();
        if (ticketId == null)
            throw new AppException("工单未持久化, 无法更新指派信息");

        LambdaUpdateWrapper<CsTicketPO> updateWrapper = new LambdaUpdateWrapper<CsTicketPO>()
                .eq(CsTicketPO::getId, ticketId)
                .eq(CsTicketPO::getUpdatedAt, expectedUpdatedAt)
                .set(CsTicketPO::getAssignedToUserId, ticket.getAssignedToUserId())
                .set(CsTicketPO::getAssignedAt, ticket.getAssignedAt());
        int affectedRowCount = csTicketMapper.update(null, updateWrapper);
        if (affectedRowCount <= 0)
            return false;

        if (ticket.getAssignedToUserId() == null)
            demoteActiveAssigneeParticipants(ticketId, null);
        else {
            demoteActiveAssigneeParticipants(ticketId, ticket.getAssignedToUserId());
            upsertActiveAgentParticipantRole(ticketId, ticket.getAssignedToUserId(), TicketParticipantRole.ASSIGNEE);
        }

        CsTicketAssignmentLogPO assignmentLogRow = toAssignmentLogPO(assignmentLog);
        int assignmentLogAffected = csTicketAssignmentLogMapper.insert(assignmentLogRow);
        require(assignmentLogAffected == 1, "写入工单指派日志失败");
        return true;
    }

    /**
     * 基于状态 CAS 更新工单状态, 并写入状态日志
     *
     * @param ticket               已完成状态推进的工单聚合
     * @param expectedFromStatus   期望旧状态
     * @param statusLog            状态日志实体
     * @return 更新是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTicketStatusWithCasAndAppendLog(@NotNull CustomerServiceTicket ticket,
                                                         @NotNull TicketStatus expectedFromStatus,
                                                         @NotNull TicketStatusLog statusLog) {
        Long ticketId = ticket.getId();
        if (ticketId == null)
            throw new AppException("工单未持久化, 无法更新状态");

        LambdaUpdateWrapper<CsTicketPO> updateWrapper = new LambdaUpdateWrapper<CsTicketPO>()
                .eq(CsTicketPO::getId, ticketId)
                .eq(CsTicketPO::getStatus, expectedFromStatus.name())
                .set(CsTicketPO::getStatus, ticket.getStatus().name())
                .set(CsTicketPO::getResolvedAt, ticket.getResolvedAt())
                .set(CsTicketPO::getClosedAt, ticket.getClosedAt())
                .set(CsTicketPO::getStatusChangedAt, ticket.getStatusChangedAt());
        int affectedRowCount = csTicketMapper.update(null, updateWrapper);
        if (affectedRowCount <= 0)
            return false;

        CsTicketStatusLogPO statusLogRow = toStatusLogPO(statusLog);
        int statusLogAffected = csTicketStatusLogMapper.insert(statusLogRow);
        require(statusLogAffected == 1, "写入工单状态日志失败");
        return true;
    }

    /**
     * 将工单内现有 ASSIGNEE 角色降级为 COLLABORATOR
     *
     * @param ticketId          工单 ID
     * @param keepAssigneeUserId 需要保留 ASSIGNEE 角色的用户 ID
     */
    private void demoteActiveAssigneeParticipants(@NotNull Long ticketId,
                                                  @Nullable Long keepAssigneeUserId) {
        LambdaUpdateWrapper<CsTicketParticipantPO> updateWrapper = new LambdaUpdateWrapper<CsTicketParticipantPO>()
                .eq(CsTicketParticipantPO::getTicketId, ticketId)
                .eq(CsTicketParticipantPO::getParticipantType, TicketParticipantType.AGENT.name())
                .eq(CsTicketParticipantPO::getRole, TicketParticipantRole.ASSIGNEE.name())
                .isNull(CsTicketParticipantPO::getLeftAt)
                .set(CsTicketParticipantPO::getRole, TicketParticipantRole.COLLABORATOR.name());
        if (keepAssigneeUserId != null)
            updateWrapper.ne(CsTicketParticipantPO::getParticipantUserId, keepAssigneeUserId);
        csTicketParticipantMapper.update(null, updateWrapper);
    }

    /**
     * 新增或更新活跃坐席参与方角色
     *
     * @param ticketId          工单 ID
     * @param participantUserId 参与方用户 ID
     * @param role              目标角色
     */
    private void upsertActiveAgentParticipantRole(@NotNull Long ticketId,
                                                  @NotNull Long participantUserId,
                                                  @NotNull TicketParticipantRole role) {
        LambdaQueryWrapper<CsTicketParticipantPO> queryWrapper = new LambdaQueryWrapper<CsTicketParticipantPO>()
                .eq(CsTicketParticipantPO::getTicketId, ticketId)
                .eq(CsTicketParticipantPO::getParticipantType, TicketParticipantType.AGENT.name())
                .eq(CsTicketParticipantPO::getParticipantUserId, participantUserId)
                .isNull(CsTicketParticipantPO::getLeftAt)
                .last("LIMIT 1");
        CsTicketParticipantPO existedRow = csTicketParticipantMapper.selectOne(queryWrapper);
        if (existedRow != null) {
            LambdaUpdateWrapper<CsTicketParticipantPO> updateWrapper = new LambdaUpdateWrapper<CsTicketParticipantPO>()
                    .eq(CsTicketParticipantPO::getId, existedRow.getId())
                    .eq(CsTicketParticipantPO::getTicketId, ticketId)
                    .set(CsTicketParticipantPO::getRole, role.name());
            csTicketParticipantMapper.update(null, updateWrapper);
            return;
        }

        CsTicketParticipantPO insertRow = CsTicketParticipantPO.builder()
                .ticketId(ticketId)
                .participantType(TicketParticipantType.AGENT.name())
                .participantUserId(participantUserId)
                .role(role.name())
                .build();
        try {
            int affectedRowCount = csTicketParticipantMapper.insert(insertRow);
            require(affectedRowCount == 1, "写入工单坐席参与方失败");
        } catch (DuplicateKeyException exception) {
            LambdaUpdateWrapper<CsTicketParticipantPO> updateWrapper = new LambdaUpdateWrapper<CsTicketParticipantPO>()
                    .eq(CsTicketParticipantPO::getTicketId, ticketId)
                    .eq(CsTicketParticipantPO::getParticipantType, TicketParticipantType.AGENT.name())
                    .eq(CsTicketParticipantPO::getParticipantUserId, participantUserId)
                    .isNull(CsTicketParticipantPO::getLeftAt)
                    .set(CsTicketParticipantPO::getRole, role.name());
            int affectedRowCount = csTicketParticipantMapper.update(null, updateWrapper);
            require(affectedRowCount >= 1, "更新工单坐席参与方失败");
        }
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
     * 持久化行转换为管理侧工单摘要视图
     *
     * @param row 持久化行
     * @return 管理侧工单摘要视图
     */
    private @NotNull AdminTicketSummaryView toAdminTicketSummaryView(@NotNull CsAdminTicketSummaryPO row) {
        return new AdminTicketSummaryView(
                requireColumn(row.getTicketId(), "ticketId"),
                requireColumn(row.getTicketNo(), "ticketNo"),
                requireColumn(row.getUserId(), "userId"),
                TicketIssueType.fromValue(requireColumn(row.getIssueType(), "issueType")),
                TicketStatus.fromValue(requireColumn(row.getStatus(), "status")),
                TicketPriority.fromValue(requireColumn(row.getPriority(), "priority")),
                TicketChannel.fromValue(requireColumn(row.getChannel(), "channel")),
                requireColumn(row.getTitle(), "title"),
                row.getOrderId(),
                row.getOrderItemId(),
                row.getShipmentId(),
                row.getAssignedToUserId(),
                row.getAssignedAt(),
                row.getLastMessageAt(),
                row.getSlaDueAt(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt")
        );
    }

    /**
     * 持久化行转换为管理侧工单详情视图
     *
     * @param row 持久化行
     * @return 管理侧工单详情视图
     */
    private @NotNull AdminTicketDetailView toAdminTicketDetailView(@NotNull CsAdminTicketDetailPO row) {
        return new AdminTicketDetailView(
                requireColumn(row.getTicketId(), "ticketId"),
                requireColumn(row.getTicketNo(), "ticketNo"),
                requireColumn(row.getUserId(), "userId"),
                TicketIssueType.fromValue(requireColumn(row.getIssueType(), "issueType")),
                TicketStatus.fromValue(requireColumn(row.getStatus(), "status")),
                TicketPriority.fromValue(requireColumn(row.getPriority(), "priority")),
                TicketChannel.fromValue(requireColumn(row.getChannel(), "channel")),
                requireColumn(row.getTitle(), "title"),
                row.getOrderId(),
                row.getOrderItemId(),
                row.getShipmentId(),
                row.getAssignedToUserId(),
                row.getAssignedAt(),
                row.getLastMessageAt(),
                row.getSlaDueAt(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt"),
                row.getDescription(),
                parseStringList(row.getAttachments(), "attachments"),
                parseStringList(row.getEvidence(), "evidence"),
                parseStringList(row.getTags(), "tags"),
                row.getRequestedRefundAmount(),
                row.getCurrency(),
                row.getClaimExternalId(),
                row.getSourceRef(),
                row.getResolvedAt(),
                row.getClosedAt()
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
     * 指派日志实体转换为指派日志持久化行
     *
     * @param assignmentLog 指派日志实体
     * @return 指派日志持久化行
     */
    private @NotNull CsTicketAssignmentLogPO toAssignmentLogPO(@NotNull TicketAssignmentLog assignmentLog) {
        return CsTicketAssignmentLogPO.builder()
                .ticketId(assignmentLog.getTicketId())
                .fromAssigneeUserId(assignmentLog.getFromAssigneeUserId())
                .toAssigneeUserId(assignmentLog.getToAssigneeUserId())
                .actionType(assignmentLog.getActionType().name())
                .actorType(assignmentLog.getActorType().name())
                .actorUserId(assignmentLog.getActorUserId())
                .sourceRef(assignmentLog.getSourceRef())
                .note(assignmentLog.getNote())
                .createdAt(assignmentLog.getCreatedAt())
                .build();
    }

    /**
     * 消息实体转换为消息持久化行
     *
     * @param message 消息实体
     * @return 消息持久化行
     */
    private @NotNull CsTicketMessagePO toTicketMessagePO(@NotNull TicketMessage message) {
        return CsTicketMessagePO.builder()
                .messageNo(message.getMessageNo().getValue())
                .ticketId(message.getTicketId())
                .senderType(message.getSenderType().name())
                .senderUserId(message.getSenderUserId())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .attachments(toJsonArray(message.getAttachments(), "message.attachments"))
                .metadata(message.getMetadata())
                .clientMessageId(message.getClientMessageId())
                .sentAt(message.getSentAt())
                .editedAt(message.getEditedAt())
                .recalledAt(message.getRecalledAt())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    /**
     * 消息持久化行转换为消息实体
     *
     * @param row 消息持久化行
     * @return 消息实体
     */
    private @NotNull TicketMessage toTicketMessageEntity(@NotNull CsTicketMessagePO row) {
        return TicketMessage.reconstitute(
                row.getId(),
                TicketMessageNo.of(requireColumn(row.getMessageNo(), "messageNo")),
                requireColumn(row.getTicketId(), "ticketId"),
                TicketParticipantType.fromValue(requireColumn(row.getSenderType(), "senderType")),
                row.getSenderUserId(),
                TicketMessageType.fromValue(requireColumn(row.getMessageType(), "messageType")),
                row.getContent(),
                parseStringList(row.getAttachments(), "message.attachments"),
                row.getMetadata(),
                row.getClientMessageId(),
                requireColumn(row.getSentAt(), "sentAt"),
                row.getEditedAt(),
                row.getRecalledAt(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt")
        );
    }

    /**
     * 消息持久化行转换为消息视图
     *
     * @param row 消息持久化行
     * @return 消息视图
     */
    private @NotNull UserTicketMessageView toUserTicketMessageView(@NotNull CsTicketMessagePO row) {
        return new UserTicketMessageView(
                requireColumn(row.getId(), "id"),
                requireColumn(row.getMessageNo(), "messageNo"),
                requireColumn(row.getTicketId(), "ticketId"),
                TicketParticipantType.fromValue(requireColumn(row.getSenderType(), "senderType")),
                row.getSenderUserId(),
                TicketMessageType.fromValue(requireColumn(row.getMessageType(), "messageType")),
                row.getContent(),
                parseStringList(row.getAttachments(), "message.attachments"),
                row.getClientMessageId(),
                requireColumn(row.getSentAt(), "sentAt"),
                row.getEditedAt(),
                row.getRecalledAt()
        );
    }

    /**
     * 参与方持久化行转换为参与方实体
     *
     * @param row 参与方持久化行
     * @return 参与方实体
     */
    private @NotNull TicketParticipant toTicketParticipantEntity(@NotNull CsTicketParticipantPO row) {
        return TicketParticipant.reconstitute(
                row.getId(),
                requireColumn(row.getTicketId(), "ticketId"),
                TicketParticipantType.fromValue(requireColumn(row.getParticipantType(), "participantType")),
                row.getParticipantUserId(),
                TicketParticipantRole.fromValue(requireColumn(row.getRole(), "role")),
                requireColumn(row.getJoinedAt(), "joinedAt"),
                row.getLeftAt(),
                row.getLastReadMessageId(),
                row.getLastReadAt(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt")
        );
    }

    /**
     * 状态日志持久化行转换为状态日志视图
     *
     * @param row 状态日志持久化行
     * @return 状态日志视图
     */
    private @NotNull UserTicketStatusLogView toUserTicketStatusLogView(@NotNull CsTicketStatusLogPO row) {
        return new UserTicketStatusLogView(
                requireColumn(row.getId(), "id"),
                requireColumn(row.getTicketId(), "ticketId"),
                row.getFromStatus() == null ? null : TicketStatus.fromValue(row.getFromStatus()),
                TicketStatus.fromValue(requireColumn(row.getToStatus(), "toStatus")),
                TicketActorType.fromValue(requireColumn(row.getActorType(), "actorType")),
                row.getActorUserId(),
                row.getSourceRef(),
                row.getNote(),
                requireColumn(row.getCreatedAt(), "createdAt")
        );
    }

    /**
     * 补发物流投影行转换为物流摘要视图
     *
     * @param row 补发物流投影行
     * @return 物流摘要视图
     */
    private @NotNull UserTicketShipmentSummaryView toUserTicketShipmentSummaryView(@NotNull CsUserTicketShipmentSummaryPO row) {
        return new UserTicketShipmentSummaryView(
                requireColumn(row.getId(), "id"),
                requireColumn(row.getShipmentNo(), "shipmentNo"),
                row.getOrderId(),
                row.getOrderNo(),
                row.getIdempotencyKey(),
                row.getCarrierCode(),
                row.getCarrierName(),
                row.getServiceCode(),
                row.getTrackingNo(),
                row.getExtExternalId(),
                ShipmentStatus.valueOf(requireColumn(row.getStatus(), "status")),
                row.getPickupTime(),
                row.getDeliveredTime(),
                normalizeCurrency(row.getCurrency()),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt")
        );
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
