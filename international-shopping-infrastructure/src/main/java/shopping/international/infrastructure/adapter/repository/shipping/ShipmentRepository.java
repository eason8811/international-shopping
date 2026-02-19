package shopping.international.infrastructure.adapter.repository.shipping;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.shipping.IShipmentRepository;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.entity.shipping.ShipmentItem;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.AddressSnapshot;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.shipping.*;
import shopping.international.infrastructure.dao.orders.OrderItemMapper;
import shopping.international.infrastructure.dao.orders.OrderStatusLogMapper;
import shopping.international.infrastructure.dao.orders.OrdersMapper;
import shopping.international.infrastructure.dao.orders.po.OrderItemPO;
import shopping.international.infrastructure.dao.orders.po.OrderStatusLogPO;
import shopping.international.infrastructure.dao.orders.po.OrdersPO;
import shopping.international.infrastructure.dao.shipping.ShipmentItemMapper;
import shopping.international.infrastructure.dao.shipping.ShipmentMapper;
import shopping.international.infrastructure.dao.shipping.ShipmentStatusLogMapper;
import shopping.international.infrastructure.dao.shipping.po.*;
import shopping.international.infrastructure.dao.user.UserAddressMapper;
import shopping.international.infrastructure.dao.user.po.UserAddressPO;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 物流仓储实现, 基于 MyBatis 和 MySQL
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ShipmentRepository implements IShipmentRepository {

    /**
     * shipment Mapper
     */
    private final ShipmentMapper shipmentMapper;
    /**
     * shipment_item Mapper
     */
    private final ShipmentItemMapper shipmentItemMapper;
    /**
     * shipment_status_log Mapper
     */
    private final ShipmentStatusLogMapper shipmentStatusLogMapper;
    /**
     * orders Mapper
     */
    private final OrdersMapper ordersMapper;
    /**
     * order_item Mapper
     */
    private final OrderItemMapper orderItemMapper;
    /**
     * order_status_log Mapper
     */
    private final OrderStatusLogMapper orderStatusLogMapper;
    /**
     * user_address Mapper
     */
    private final UserAddressMapper userAddressMapper;
    /**
     * JSON 工具
     */
    private final ObjectMapper objectMapper;

    /**
     * 查询用户侧订单关联物流单详情列表
     *
     * @param userId      用户主键
     * @param orderNo     订单号
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情列表
     */
    @Override
    public @NotNull List<Shipment> listUserOrderShipments(@NotNull Long userId,
                                                          @NotNull OrderNo orderNo,
                                                          boolean includeLogs) {
        List<ShipmentPO> rows = includeLogs
                ? shipmentMapper.selectUserShipmentDetailsWithItemsAndLogsByOrderNo(userId, orderNo.getValue())
                : shipmentMapper.selectUserShipmentDetailsWithItemsByOrderNo(userId, orderNo.getValue());
        if (rows == null || rows.isEmpty())
            return List.of();
        return rows.stream()
                .map(row -> toShipment(
                                row,
                                detailItems(row),
                                includeLogs ? detailLogs(row) : List.of()
                        )
                )
                .toList();
    }

    /**
     * 查询用户侧物流单详情
     *
     * @param userId     用户主键
     * @param shipmentNo 物流单号
     * @return 物流单详情, 不存在时为空
     */
    @Override
    public @NotNull Optional<Shipment> findUserShipmentDetail(@NotNull Long userId,
                                                              @NotNull ShipmentNo shipmentNo) {
        ShipmentPO row = shipmentMapper.selectUserShipmentDetailWithItemsAndLogsByNo(userId, shipmentNo.getValue());
        if (row == null)
            return Optional.empty();
        return Optional.of(toShipment(row, detailItems(row), detailLogs(row)));
    }

    /**
     * 管理侧分页查询物流单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<ShipmentSummaryView> pageShipments(@NotNull ShipmentPageCriteria criteria,
                                                                  @NotNull PageQuery pageQuery) {
        List<String> statusIn = criteria.getStatusIn() == null
                ? List.of()
                : criteria.getStatusIn().stream().map(Enum::name).toList();

        List<ShipmentPO> rows = shipmentMapper.pageAdminShipments(
                criteria.getShipmentNo(),
                criteria.getOrderNo(),
                criteria.getOrderId(),
                criteria.getCarrierCode(),
                criteria.getTrackingNo(),
                criteria.getExtExternalId(),
                statusIn,
                criteria.getUpdatedFrom(),
                criteria.getUpdatedTo(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo(),
                criteria.getSortField(),
                criteria.getSortDirection(),
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = shipmentMapper.countAdminShipments(
                criteria.getShipmentNo(),
                criteria.getOrderNo(),
                criteria.getOrderId(),
                criteria.getCarrierCode(),
                criteria.getTrackingNo(),
                criteria.getExtExternalId(),
                statusIn,
                criteria.getUpdatedFrom(),
                criteria.getUpdatedTo(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo()
        );

        if (rows == null || rows.isEmpty())
            return PageResult.<ShipmentSummaryView>builder()
                    .items(List.of())
                    .total(total)
                    .build();

        List<ShipmentSummaryView> items = rows.stream().map(this::toSummaryView).toList();
        return PageResult.<ShipmentSummaryView>builder()
                .items(items)
                .total(total)
                .build();
    }

    /**
     * 管理侧按主键查询物流单详情
     *
     * @param shipmentId  物流单主键
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情, 不存在时为空
     */
    @Override
    public @NotNull Optional<Shipment> findShipmentDetailById(@NotNull Long shipmentId,
                                                              boolean includeLogs) {
        ShipmentPO row = shipmentMapper.selectDetailById(shipmentId);
        if (row == null)
            return Optional.empty();
        return Optional.of(assembleDetail(row, includeLogs));
    }

    /**
     * 按追踪号查询物流单详情
     *
     * @param trackingNo  追踪号
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情, 不存在时为空
     */
    @Override
    public @NotNull Optional<Shipment> findShipmentDetailByTrackingNo(@NotNull String trackingNo,
                                                                      boolean includeLogs) {
        ShipmentPO row = shipmentMapper.selectDetailByTrackingNo(trackingNo);
        if (row == null)
            return Optional.empty();
        return Optional.of(assembleDetail(row, includeLogs));
    }

    /**
     * 管理侧分页查询物流状态日志
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<ShipmentStatusLog> pageStatusLogs(@NotNull ShipmentStatusLogPageCriteria criteria,
                                                                 @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();

        List<ShipmentStatusLogPO> rows = shipmentStatusLogMapper.pageLogs(
                criteria.getShipmentId(),
                criteria.getOrderNo(),
                criteria.getFromStatus() == null ? null : criteria.getFromStatus().name(),
                criteria.getToStatus() == null ? null : criteria.getToStatus().name(),
                criteria.getSourceType() == null ? null : criteria.getSourceType().name(),
                criteria.getSourceRef(),
                criteria.getCarrierCode(),
                criteria.getTrackingNo(),
                criteria.getEventTimeFrom(),
                criteria.getEventTimeTo(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo(),
                criteria.getSortField(),
                criteria.getSortDirection(),
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = shipmentStatusLogMapper.countLogs(
                criteria.getShipmentId(),
                criteria.getOrderNo(),
                criteria.getFromStatus() == null ? null : criteria.getFromStatus().name(),
                criteria.getToStatus() == null ? null : criteria.getToStatus().name(),
                criteria.getSourceType() == null ? null : criteria.getSourceType().name(),
                criteria.getSourceRef(),
                criteria.getCarrierCode(),
                criteria.getTrackingNo(),
                criteria.getEventTimeFrom(),
                criteria.getEventTimeTo(),
                criteria.getCreatedFrom(),
                criteria.getCreatedTo()
        );
        if (rows == null || rows.isEmpty())
            return PageResult.<ShipmentStatusLog>builder().items(List.of()).total(total).build();

        List<ShipmentStatusLog> items = rows.stream().map(this::toStatusLogEntity).toList();
        return PageResult.<ShipmentStatusLog>builder().items(items).total(total).build();
    }

    /**
     * 回填物流面单, 并记录状态日志
     *
     * @param shipmentId        物流单主键
     * @param label             面单信息
     * @param shipFromAddressId 物流单寄出地址 ID (user_address.id)
     * @param idempotencyKey    请求幂等键
     * @param sourceRef         来源引用
     * @param actorUserId       操作者主键
     * @param note              备注
     * @return 更新后的物流单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull FillLabelResult fillLabel(@NotNull Long shipmentId,
                                              @NotNull ShipmentLabel label,
                                              @NotNull Integer shipFromAddressId,
                                              @NotNull String idempotencyKey,
                                              @NotNull String sourceRef,
                                              @Nullable Long actorUserId,
                                              @Nullable String note) {
        requireNotBlank(idempotencyKey, "idempotencyKey 不能为空");
        requireNotBlank(sourceRef, "sourceRef 不能为空");

        Shipment shipment = findShipmentDetailById(shipmentId, true)
                .orElseThrow(() -> new NotFoundException("物流单不存在"));
        ShipmentStatusLog existingLog = shipment.getStatusLogList().stream()
                .filter(log -> log.getSourceType() == ShipmentStatusEventSource.API
                        && Objects.equals(log.getSourceRef(), sourceRef))
                .findFirst()
                .orElse(null);
        if (existingLog != null)
            return new FillLabelResult(shipment, true);

        ShipmentStatus oldStatus = shipment.getStatus();
        LocalDateTime oldUpdatedAt = shipment.getUpdatedAt();

        shipment.fillLabel(label);

        ShipmentTrackingEvent event = ShipmentTrackingEvent.keepCurrent(
                LocalDateTime.now(),
                ShipmentStatusEventSource.API,
                sourceRef,
                shipment.getCarrierCode(),
                shipment.getTrackingNo(),
                note,
                Map.of("idempotency_key", idempotencyKey),
                actorUserId
        );

        UserAddressPO userAddressPO = userAddressMapper.selectById(shipFromAddressId);
        if (userAddressPO == null)
            throw new IllegalParamException("ID 为 '" + shipFromAddressId + "' 的发货地址不存在");
        ShippingAddressSnapshot shipFrom = buildShipFrom(userAddressPO);
        shipment.bindAddressSnapshots(shipFrom, shipment.getShipTo());

        ShipmentStatusLog appendedLog = shipment.applyTrackingEvent(event);
        int updated = updateShipmentWithStatusCas(shipment, oldStatus, oldUpdatedAt);
        if (updated <= 0) {
            Shipment reloaded = findShipmentDetailById(shipmentId, true)
                    .orElseThrow(() -> new ConflictException("物流单并发更新失败"));
            boolean hasLog = reloaded.getStatusLogList().stream()
                    .anyMatch(log -> log.getSourceType() == ShipmentStatusEventSource.API
                            && Objects.equals(log.getSourceRef(), sourceRef));
            if (!hasLog)
                throw new ConflictException("物流单并发更新失败");
            return new FillLabelResult(reloaded, true);
        }

        insertStatusLogIgnoreDuplicate(appendedLog);
        Shipment reloaded = findShipmentDetailById(shipmentId, true)
                .orElseThrow(() -> new ConflictException("物流单回读失败"));
        return new FillLabelResult(reloaded, false);
    }

    /**
     * 批量发货, 每个物流单统一通过聚合 applyTrackingEvent 推进状态
     *
     * @param shipmentIds    物流单主键列表
     * @param idempotencyKey 请求幂等键
     * @param sourceRef      来源引用
     * @param note           备注
     * @param actorUserId    操作者主键
     * @return 更新后的物流单列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull List<Shipment> dispatch(@NotNull List<Long> shipmentIds,
                                            @NotNull String idempotencyKey,
                                            @NotNull String sourceRef,
                                            @NotNull String note,
                                            @Nullable Long actorUserId) {
        require(!shipmentIds.isEmpty(), "shipmentIds 不能为空");
        List<Long> dedupIds = shipmentIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, ShipmentPO> detailMap = loadShipmentDetailsByIds(dedupIds, true);
        if (detailMap.size() != dedupIds.size())
            throw new NotFoundException("物流单不存在");

        List<ShipmentDispatchStatusCasPO> casRows = new ArrayList<>();
        List<ShipmentStatusLogPO> logPos = new ArrayList<>();
        for (Long shipmentId : dedupIds) {
            ShipmentPO detailRow = detailMap.get(shipmentId);
            if (detailRow == null)
                throw new NotFoundException("物流单不存在");

            Shipment shipment = toShipment(
                    detailRow,
                    detailItems(detailRow),
                    detailLogs(detailRow)
            );

            String rowSourceRef = sourceRef + ":" + shipmentId;
            boolean replayed = shipment.getStatusLogList().stream()
                    .anyMatch(log -> log.getSourceType() == ShipmentStatusEventSource.MANUAL
                            && Objects.equals(log.getSourceRef(), rowSourceRef));
            if (replayed)
                continue;

            ShipmentStatus oldStatus = shipment.getStatus();
            ShipmentStatusLog appendedLog = shipment.dispatch(
                    ShipmentStatusEventSource.MANUAL,
                    rowSourceRef,
                    note,
                    LocalDateTime.now(),
                    Map.of("idempotency_key", idempotencyKey),
                    actorUserId
            );
            casRows.add(
                    ShipmentDispatchStatusCasPO.builder()
                            .shipmentId(shipmentId)
                            .oldStatus(oldStatus.name())
                            .newStatus(shipment.getStatus().name())
                            .build()
            );
            logPos.add(toStatusLogPO(appendedLog));
        }

        if (!casRows.isEmpty()) {
            int updated = shipmentMapper.batchUpdateStatusWithCas(casRows);
            if (updated != casRows.size()) {
                Map<Long, ShipmentPO> reloadedMap = loadShipmentDetailsByIds(dedupIds, true);
                if (reloadedMap.size() != dedupIds.size())
                    throw new ConflictException("物流单并发更新失败");

                boolean allReplayed = true;
                for (Long shipmentId : dedupIds) {
                    ShipmentPO reloadedRow = reloadedMap.get(shipmentId);
                    if (reloadedRow == null) {
                        allReplayed = false;
                        break;
                    }
                    Shipment reloaded = toShipment(
                            reloadedRow,
                            detailItems(reloadedRow),
                            detailLogs(reloadedRow)
                    );
                    String rowSourceRef = sourceRef + ":" + shipmentId;
                    boolean hasLog = reloaded.getStatusLogList().stream()
                            .anyMatch(log -> log.getSourceType() == ShipmentStatusEventSource.MANUAL
                                    && Objects.equals(log.getSourceRef(), rowSourceRef));
                    if (!hasLog) {
                        allReplayed = false;
                        break;
                    }
                }
                if (!allReplayed)
                    throw new ConflictException("物流单并发更新失败");

                return dedupIds.stream()
                        .map(id -> {
                            ShipmentPO row = reloadedMap.get(id);
                            if (row == null)
                                throw new ConflictException("物流单回读失败");
                            return toShipment(row, detailItems(row), detailLogs(row));
                        })
                        .toList();
            }
        }

        if (!logPos.isEmpty())
            shipmentStatusLogMapper.batchInsert(logPos);

        Map<Long, ShipmentPO> finalMap = loadShipmentDetailsByIds(dedupIds, true);
        if (finalMap.size() != dedupIds.size())
            throw new ConflictException("物流单回读失败");

        return dedupIds.stream()
                .map(id -> {
                    ShipmentPO row = finalMap.get(id);
                    if (row == null)
                        throw new ConflictException("物流单回读失败");
                    return toShipment(row, detailItems(row), detailLogs(row));
                })
                .toList();
    }

    /**
     * 管理侧手工创建物流单
     *
     * @param command               手工创建命令
     * @param requestIdempotencyKey 请求幂等键
     * @param actorUserId           操作者主键
     * @return 创建后的物流单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Shipment manualCreate(@NotNull ManualCreateShipmentCommand command,
                                          @NotNull String requestIdempotencyKey,
                                          @Nullable Long actorUserId) {
        command.validate();
        requireNotBlank(requestIdempotencyKey, "requestIdempotencyKey 不能为空");

        OrdersPO order = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getOrderNo, command.getOrderNo().getValue())
                .last("limit 1"));
        if (order == null)
            throw new NotFoundException("订单不存在");
        if (!OrderStatus.PAID.name().equals(order.getStatus()))
            throw new ConflictException("仅已支付订单允许手工补建物流单");

        ShipmentPO existing = shipmentMapper.selectOne(new LambdaQueryWrapper<ShipmentPO>()
                .eq(ShipmentPO::getOrderId, order.getId())
                .last("limit 1"));
        if (existing != null) {
            if (existing.getIdempotencyKey() != null && existing.getIdempotencyKey().equals(requestIdempotencyKey))
                return assembleDetail(existing, true);
            throw new ConflictException("订单已存在关联物流单");
        }

        String sourceRef = "admin:shipment:manual:create:" + requestIdempotencyKey;
        Shipment shipment = ensurePlaceholderForPaidOrder(
                order.getId(),
                order.getOrderNo(),
                requestIdempotencyKey,
                sourceRef,
                ShipmentStatusEventSource.MANUAL,
                command.getNote(),
                actorUserId
        );
        if (shipment.getId() == null)
            throw new NotFoundException("占位物流单 ID 缺失");

        boolean hasLabelFields = command.getCarrierCode() != null && !command.getCarrierCode().isBlank()
                && command.getCarrierName() != null && !command.getCarrierName().isBlank()
                && command.getTrackingNo() != null && !command.getTrackingNo().isBlank();
        if (!hasLabelFields)
            return shipment;

        ShipmentDimension dimension = ShipmentDimension.ofText(
                command.getWeightKg(),
                command.getLengthCm(),
                command.getWidthCm(),
                command.getHeightCm()
        );
        ShipmentLabel label = ShipmentLabel.of(
                command.getCarrierCode(),
                command.getCarrierName(),
                command.getServiceCode(),
                command.getTrackingNo(),
                command.getExtExternalId(),
                command.getLabelUrl(),
                dimension,
                command.getDeclaredValue(),
                command.getCurrency()
        );


        return fillLabel(
                shipment.getId(),
                label,
                command.getShipFromAddressId(),
                requestIdempotencyKey,
                sourceRef + ":" + shipment.getId(),
                actorUserId,
                command.getNote()
        ).shipment();
    }

    /**
     * 应用轨迹事件并持久化
     *
     * @param shipmentId 物流单主键
     * @param event      轨迹事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyTrackingEvent(@NotNull Long shipmentId, @NotNull ShipmentTrackingEvent event) {
        Shipment shipment = findShipmentDetailById(shipmentId, true)
                .orElseThrow(() -> new NotFoundException("物流单不存在"));
        ShipmentStatus oldStatus = shipment.getStatus();
        LocalDateTime oldUpdatedAt = shipment.getUpdatedAt();

        ShipmentStatusLog appendedLog = shipment.applyTrackingEvent(event);
        int updated = updateShipmentWithStatusCas(shipment, oldStatus, oldUpdatedAt);
        if (updated <= 0) {
            Shipment reloaded = findShipmentDetailById(shipmentId, true)
                    .orElseThrow(() -> new ConflictException("物流单并发更新失败"));
            boolean hasLog = reloaded.getStatusLogList().stream()
                    .anyMatch(log -> log.getSourceType() == event.getSourceType()
                            && Objects.equals(log.getSourceRef(), event.getSourceRef()));
            if (!hasLog)
                throw new ConflictException("物流单并发更新失败");
        }

        insertStatusLogIgnoreDuplicate(appendedLog);

        if (shipment.getOrderId() != null && shipment.getStatus() == ShipmentStatus.DELIVERED)
            tryAdvanceOrderToFulfilled(shipment.getOrderId());

        findShipmentDetailById(shipmentId, true)
                .orElseThrow(() -> new ConflictException("物流单回读失败"));
    }

    /**
     * PAID 订单补建占位物流单, 用于支付链路或补偿任务
     *
     * @param orderId                订单主键
     * @param orderNo                订单号
     * @param shipmentIdempotencyKey 物流单幂等键
     * @param sourceRef              状态日志来源引用
     * @param sourceType             状态日志来源类型
     * @param note                   状态日志备注
     * @param actorUserId            操作者主键
     * @return 创建或复用后的物流单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Shipment ensurePlaceholderForPaidOrder(@NotNull Long orderId,
                                                           @NotNull String orderNo,
                                                           @NotNull String shipmentIdempotencyKey,
                                                           @NotNull String sourceRef,
                                                           @NotNull ShipmentStatusEventSource sourceType,
                                                           @Nullable String note,
                                                           @Nullable Long actorUserId) {
        requireNotBlank(orderNo, "orderNo 不能为空");
        requireNotBlank(shipmentIdempotencyKey, "shipmentIdempotencyKey 不能为空");

        ShipmentPO existing = shipmentMapper.selectOne(new LambdaQueryWrapper<ShipmentPO>()
                .eq(ShipmentPO::getOrderId, orderId)
                .last("limit 1"));
        if (existing != null)
            return assembleDetail(existing, true);

        OrdersPO order = ordersMapper.selectById(orderId);
        if (order == null)
            throw new NotFoundException("订单不存在");
        if (!OrderStatus.PAID.name().equals(order.getStatus()))
            throw new ConflictException("仅已支付订单允许创建占位物流单");

        List<OrderItemPO> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getOrderId, orderId)
                .orderByAsc(OrderItemPO::getId));
        require(orderItems != null && !orderItems.isEmpty(), "订单明细不能为空");

        ShippingAddressSnapshot shipTo = buildShipTo(order);

        List<ShipmentItem> shipmentItems = orderItems.stream()
                .map(this::toShipmentItemEntity)
                .toList();

        Shipment shipment = Shipment.createPlaceholder(
                ShipmentNo.generate(),
                orderId,
                orderNo,
                shipmentIdempotencyKey,
                null,
                shipTo,
                order.getTotalAmount(),
                order.getCurrency(),
                shipmentItems,
                CustomsInfoSnapshot.empty()
        );

        ShipmentPO shipmentPO = toShipmentPO(shipment);
        try {
            shipmentMapper.insert(shipmentPO);
        } catch (DuplicateKeyException duplicateKeyException) {
            ShipmentPO byIdempotency = shipmentMapper.selectOne(
                    new LambdaQueryWrapper<ShipmentPO>()
                            .eq(ShipmentPO::getOrderId, orderId)
                            .eq(ShipmentPO::getIdempotencyKey, shipmentIdempotencyKey)
                            .last("limit 1")
            );
            if (byIdempotency != null)
                return assembleDetail(byIdempotency, true);

            ShipmentPO byOrder = shipmentMapper.selectOne(
                    new LambdaQueryWrapper<ShipmentPO>()
                            .eq(ShipmentPO::getOrderId, orderId)
                            .last("limit 1")
            );
            if (byOrder != null)
                return assembleDetail(byOrder, true);
            throw duplicateKeyException;
        }

        shipment.assignId(shipmentPO.getId());

        ShipmentTrackingEvent createdEvent = ShipmentTrackingEvent.transition(
                ShipmentStatus.CREATED,
                LocalDateTime.now(),
                sourceType,
                sourceRef,
                shipment.getCarrierCode(),
                shipment.getTrackingNo(),
                note,
                Map.of("order_no", orderNo),
                actorUserId
        );
        ShipmentStatusLog createdLog = shipment.applyTrackingEvent(createdEvent);

        List<ShipmentItemPO> itemPOs = shipment.getItemList().stream().map(this::toShipmentItemPO).toList();
        if (!itemPOs.isEmpty())
            shipmentItemMapper.batchInsert(itemPOs);

        insertStatusLogIgnoreDuplicate(createdLog);

        return shipment;
    }

    /**
     * 补偿任务, 扫描并补建 PAID 且无物流单的订单
     *
     * @param limit           批次数量
     * @param sourceRefPrefix 来源引用前缀
     * @return 本批补建数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int compensatePaidOrdersWithoutShipment(int limit, @NotNull String sourceRefPrefix) {
        requireNotBlank(sourceRefPrefix, "sourceRefPrefix 不能为空");
        int safeLimit = Math.max(limit, 1);
        List<PaidOrderCandidatePO> candidates = shipmentMapper.listPaidOrdersWithoutShipment(safeLimit);
        if (candidates == null || candidates.isEmpty())
            return 0;

        List<Long> orderIds = candidates.stream()
                .map(PaidOrderCandidatePO::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty())
            return 0;

        List<OrdersPO> orders = ordersMapper.selectByIds(orderIds);
        if (orders == null || orders.isEmpty())
            return 0;
        Map<Long, OrdersPO> orderMap = orders.stream()
                .collect(Collectors.toMap(OrdersPO::getId, Function.identity()));

        List<OrderItemPO> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .in(OrderItemPO::getOrderId, orderIds)
                .orderByAsc(OrderItemPO::getOrderId)
                .orderByAsc(OrderItemPO::getId));
        Map<Long, List<OrderItemPO>> orderItemMap = orderItems == null ? Map.of() : orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemPO::getOrderId));

        List<ShipmentPO> inserts = new ArrayList<>();
        for (PaidOrderCandidatePO candidate : candidates) {
            OrdersPO order = orderMap.get(candidate.getOrderId());
            if (order == null)
                continue;
            List<OrderItemPO> items = orderItemMap.get(candidate.getOrderId());
            if (items == null || items.isEmpty())
                continue;

            try {
                inserts.add(buildCompensationPlaceholder(order, candidate.getOrderId()));
            } catch (Exception exception) {
                log.warn("补偿构造占位物流单失败, orderId: {}, err: {}",
                        candidate.getOrderId(), exception.getMessage(), exception);
            }
        }
        if (inserts.isEmpty())
            return 0;

        shipmentMapper.batchInsertIgnore(inserts);

        List<String> idempotencyKeys = inserts.stream()
                .map(ShipmentPO::getIdempotencyKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<ShipmentPO> persisted = shipmentMapper.selectList(
                new LambdaQueryWrapper<ShipmentPO>()
                        .in(ShipmentPO::getOrderId, orderIds)
                        .in(ShipmentPO::getIdempotencyKey, idempotencyKeys)
        );
        if (persisted == null || persisted.isEmpty())
            return 0;

        Map<Long, ShipmentPO> shipmentByOrder = persisted.stream()
                .collect(Collectors.toMap(
                        ShipmentPO::getOrderId,
                        it -> it,
                        (left, right) -> right
                ));
        List<ShipmentItemPO> shipmentItems = new ArrayList<>();
        List<ShipmentStatusLogPO> logs = new ArrayList<>();

        for (PaidOrderCandidatePO candidate : candidates) {
            ShipmentPO shipmentPO = shipmentByOrder.get(candidate.getOrderId());
            if (shipmentPO == null)
                continue;

            List<OrderItemPO> items = orderItemMap.get(candidate.getOrderId());
            if (items == null || items.isEmpty())
                continue;
            List<ShipmentItem> shipmentItemEntities = items.stream()
                    .map(this::toShipmentItemEntity)
                    .toList();
            for (OrderItemPO item : items)
                shipmentItems.add(
                        ShipmentItemPO.builder()
                                .shipmentId(shipmentPO.getId())
                                .orderId(item.getOrderId())
                                .orderItemId(item.getId())
                                .productId(item.getProductId())
                                .skuId(item.getSkuId())
                                .quantity(item.getQuantity())
                                .build()
                );

            Shipment shipment = Shipment.createPlaceholder(
                    ShipmentNo.of(shipmentPO.getShipmentNo()),
                    shipmentPO.getOrderId(),
                    candidate.getOrderNo(),
                    shipmentPO.getIdempotencyKey(),
                    parseShippingAddress(shipmentPO.getShipFrom()),
                    parseShippingAddress(shipmentPO.getShipTo()),
                    shipmentPO.getDeclaredValue(),
                    shipmentPO.getCurrency(),
                    shipmentItemEntities,
                    parseCustomsInfo(shipmentPO.getCustomsInfo())
            );
            shipment.assignId(shipmentPO.getId());
            ShipmentStatusLog createdLog = shipment.applyTrackingEvent(
                    ShipmentTrackingEvent.transition(
                            ShipmentStatus.CREATED,
                            LocalDateTime.now(),
                            ShipmentStatusEventSource.SYSTEM_JOB,
                            sourceRefPrefix + ":" + candidate.getOrderId(),
                            null,
                            null,
                            "PAID 订单补偿补建物流单",
                            candidate.getOrderNo() == null ? Map.of() : Map.of("order_no", candidate.getOrderNo()),
                            null
                    )
            );
            logs.add(toStatusLogPO(createdLog));
        }

        if (!shipmentItems.isEmpty())
            shipmentItemMapper.batchInsert(shipmentItems);
        if (!logs.isEmpty())
            shipmentStatusLogMapper.batchInsert(logs);

        return logs.size();
    }

    /**
     * 判断订单是否存在不允许改址的物流单
     *
     * @param orderId 订单主键
     * @return true 表示存在不允许改址的物流单
     */
    @Override
    public boolean existsAddressChangeForbiddenShipment(@NotNull Long orderId) {
        Long count = shipmentMapper.selectCount(new LambdaQueryWrapper<ShipmentPO>()
                .eq(ShipmentPO::getOrderId, orderId)
                .notIn(ShipmentPO::getStatus, ShipmentStatus.CREATED.name(), ShipmentStatus.LABEL_CREATED.name())
                .last("limit 1"));
        return count != null && count > 0;
    }

    /**
     * 组装单个详情
     *
     * @param row         基础行
     * @param includeLogs 是否包含日志
     * @return 详情
     */
    private @NotNull Shipment assembleDetail(@NotNull ShipmentPO row,
                                             boolean includeLogs) {
        Long shipmentId = row.getId();
        Map<Long, ShipmentPO> detailMap = loadShipmentDetailsByIds(List.of(shipmentId), includeLogs);

        ShipmentPO detailRow = detailMap.get(row.getId());
        ShipmentPO shipmentRow = detailRow == null ? row : detailRow;

        return toShipment(
                shipmentRow,
                detailItems(detailRow),
                includeLogs ? detailLogs(detailRow) : List.of()
        );
    }

    /**
     * 按主键集合联表加载物流详情
     *
     * @param shipmentIds 物流单主键集合
     * @param includeLogs 是否加载状态日志
     * @return 主键到详情行映射
     */
    private @NotNull Map<Long, ShipmentPO> loadShipmentDetailsByIds(@NotNull List<Long> shipmentIds,
                                                                    boolean includeLogs) {
        if (shipmentIds.isEmpty())
            return Map.of();
        List<ShipmentPO> rows = includeLogs
                ? shipmentMapper.selectDetailWithItemsAndLogsByShipmentIds(shipmentIds)
                : shipmentMapper.selectDetailWithItemsByShipmentIds(shipmentIds);
        if (rows == null || rows.isEmpty())
            return Map.of();

        Map<Long, ShipmentPO> detailMap = new LinkedHashMap<>();
        for (ShipmentPO row : rows) {
            ShipmentPO existing = detailMap.get(row.getId());
            if (existing == null) {
                detailMap.put(row.getId(), row);
                continue;
            }
            existing.setItems(mergeItemRows(existing.getItems(), row.getItems()));
            existing.setStatusLogs(mergeLogRows(existing.getStatusLogs(), row.getStatusLogs()));
            if (existing.getOrderNo() == null)
                existing.setOrderNo(row.getOrderNo());
        }
        return detailMap;
    }

    /**
     * 读取联表详情中的物流明细
     *
     * @param row 联表详情行
     * @return 明细列表
     */
    private @NotNull List<ShipmentItemPO> detailItems(@Nullable ShipmentPO row) {
        if (row == null || row.getItems() == null)
            return List.of();
        return row.getItems();
    }

    /**
     * 读取联表详情中的状态日志
     *
     * @param row 联表详情行
     * @return 状态日志列表
     */
    private @NotNull List<ShipmentStatusLogPO> detailLogs(@Nullable ShipmentPO row) {
        if (row == null || row.getStatusLogs() == null)
            return List.of();
        return row.getStatusLogs();
    }

    /**
     * 合并物流明细行, 按主键去重
     *
     * @param left  左侧集合
     * @param right 右侧集合
     * @return 合并后的集合
     */
    private @NotNull List<ShipmentItemPO> mergeItemRows(@Nullable List<ShipmentItemPO> left,
                                                        @Nullable List<ShipmentItemPO> right) {
        if ((left == null || left.isEmpty()) && (right == null || right.isEmpty()))
            return List.of();
        if (left == null || left.isEmpty())
            return right;
        if (right == null || right.isEmpty())
            return left;

        Map<Long, ShipmentItemPO> merged = new LinkedHashMap<>();
        for (ShipmentItemPO row : left) {
            if (row == null || row.getId() == null)
                continue;
            merged.put(row.getId(), row);
        }
        for (ShipmentItemPO row : right) {
            if (row == null || row.getId() == null)
                continue;
            merged.putIfAbsent(row.getId(), row);
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 合并状态日志行, 按主键去重
     *
     * @param left  左侧集合
     * @param right 右侧集合
     * @return 合并后的集合
     */
    private @NotNull List<ShipmentStatusLogPO> mergeLogRows(@Nullable List<ShipmentStatusLogPO> left,
                                                            @Nullable List<ShipmentStatusLogPO> right) {
        if ((left == null || left.isEmpty()) && (right == null || right.isEmpty()))
            return List.of();
        if (left == null || left.isEmpty())
            return right;
        if (right == null || right.isEmpty())
            return left;

        Map<Long, ShipmentStatusLogPO> merged = new LinkedHashMap<>();
        for (ShipmentStatusLogPO row : left) {
            if (row == null || row.getId() == null)
                continue;
            merged.put(row.getId(), row);
        }
        for (ShipmentStatusLogPO row : right) {
            if (row == null || row.getId() == null)
                continue;
            merged.putIfAbsent(row.getId(), row);
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 将持久化对象转换为摘要视图
     *
     * @param po 持久化对象
     * @return 摘要视图
     */
    private @NotNull ShipmentSummaryView toSummaryView(@NotNull ShipmentPO po) {
        return new ShipmentSummaryView(
                po.getId(),
                po.getShipmentNo(),
                po.getOrderId(),
                po.getOrderNo(),
                po.getIdempotencyKey(),
                po.getCarrierCode(),
                po.getCarrierName(),
                po.getServiceCode(),
                po.getTrackingNo(),
                po.getExtExternalId(),
                ShipmentStatus.valueOf(po.getStatus()),
                po.getPickupTime(),
                po.getDeliveredTime(),
                po.getCurrency(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 转换为物流聚合
     *
     * @param po       shipment 行
     * @param itemRows 明细行
     * @param logRows  日志行
     * @return 物流聚合
     */
    private @NotNull Shipment toShipment(@NotNull ShipmentPO po,
                                         @NotNull List<ShipmentItemPO> itemRows,
                                         @NotNull List<ShipmentStatusLogPO> logRows) {
        List<ShipmentItem> itemList = itemRows.stream().map(this::toShipmentItemEntity).toList();
        List<ShipmentStatusLog> statusLogs = logRows.stream().map(this::toStatusLogEntity).toList();

        return Shipment.reconstitute(
                po.getId(),
                ShipmentNo.of(po.getShipmentNo()),
                po.getOrderId(),
                po.getOrderNo(),
                po.getIdempotencyKey(),
                po.getCarrierCode(),
                po.getCarrierName(),
                po.getServiceCode(),
                po.getTrackingNo(),
                po.getExtExternalId(),
                ShipmentStatus.valueOf(po.getStatus()),
                parseShippingAddress(po.getShipFrom()),
                parseShippingAddress(po.getShipTo()),
                ShipmentDimension.of(po.getWeightKg(), po.getLengthCm(), po.getWidthCm(), po.getHeightCm()),
                po.getDeclaredValue(),
                po.getCurrency(),
                parseCustomsInfo(po.getCustomsInfo()),
                po.getLabelUrl(),
                po.getPickupTime(),
                po.getDeliveredTime(),
                itemList,
                statusLogs,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 执行带状态 CAS 的物流单更新
     *
     * @param shipment  物流聚合
     * @param oldStatus 旧状态
     * @return 更新行数
     */
    private int updateShipmentWithStatusCas(@NotNull Shipment shipment,
                                            @NotNull ShipmentStatus oldStatus,
                                            @NotNull LocalDateTime oldUpdatedAt) {
        ShipmentDimension dimension = shipment.getDimension();
        return shipmentMapper.update(null, new LambdaUpdateWrapper<ShipmentPO>()
                .eq(ShipmentPO::getId, shipment.getId())
                .eq(ShipmentPO::getStatus, oldStatus.name())
                .eq(ShipmentPO::getUpdatedAt, oldUpdatedAt)
                .set(ShipmentPO::getCarrierCode, shipment.getCarrierCode())
                .set(ShipmentPO::getCarrierName, shipment.getCarrierName())
                .set(ShipmentPO::getServiceCode, shipment.getServiceCode())
                .set(ShipmentPO::getTrackingNo, shipment.getTrackingNo())
                .set(ShipmentPO::getExtExternalId, shipment.getExtExternalId())
                .set(ShipmentPO::getStatus, shipment.getStatus().name())
                .set(ShipmentPO::getShipFrom, toJsonOrNull(shipment.getShipFrom()))
                .set(ShipmentPO::getShipTo, toJsonOrNull(shipment.getShipTo()))
                .set(ShipmentPO::getWeightKg, dimension == null ? null : dimension.getWeightKg())
                .set(ShipmentPO::getLengthCm, dimension == null ? null : dimension.getLengthCm())
                .set(ShipmentPO::getWidthCm, dimension == null ? null : dimension.getWidthCm())
                .set(ShipmentPO::getHeightCm, dimension == null ? null : dimension.getHeightCm())
                .set(ShipmentPO::getDeclaredValue, shipment.getDeclaredValue())
                .set(ShipmentPO::getCurrency, shipment.getCurrency())
                .set(ShipmentPO::getCustomsInfo, toJsonOrNull(shipment.getCustomsInfo() == null ? null : shipment.getCustomsInfo().getExtra()))
                .set(ShipmentPO::getLabelUrl, shipment.getLabelUrl())
                .set(ShipmentPO::getPickupTime, shipment.getPickupTime())
                .set(ShipmentPO::getDeliveredTime, shipment.getDeliveredTime()));
    }

    /**
     * 插入状态日志, 遇到去重冲突时忽略
     *
     * @param statusLog 状态日志实体
     */
    private void insertStatusLogIgnoreDuplicate(@NotNull ShipmentStatusLog statusLog) {
        try {
            shipmentStatusLogMapper.insert(toStatusLogPO(statusLog));
        } catch (DuplicateKeyException ignore) {
            // 去重索引命中时视为幂等成功
        }
    }

    /**
     * 尝试推进订单为 FULFILLED
     *
     * @param orderId 订单主键
     */
    private void tryAdvanceOrderToFulfilled(@NotNull Long orderId) {
        int updated = shipmentMapper.tryAdvanceOrderToFulfilled(orderId);
        if (updated <= 0)
            return;

        orderStatusLogMapper.insert(OrderStatusLogPO.builder()
                .orderId(orderId)
                .eventSource(OrderStatusEventSource.SHIPPING_CALLBACK.name())
                .fromStatus(OrderStatus.PAID.name())
                .toStatus(OrderStatus.FULFILLED.name())
                .note("订单关联物流单全部签收, 自动推进履约完成")
                .build());
    }

    /**
     * 构造补偿任务使用的占位物流单持久化对象
     *
     * @param order   订单行
     * @param orderId 订单主键
     * @return 占位物流单持久化对象
     */
    private @NotNull ShipmentPO buildCompensationPlaceholder(@NotNull OrdersPO order,
                                                             @NotNull Long orderId) {
        return ShipmentPO.builder()
                .shipmentNo(ShipmentNo.generate().getValue())
                .orderId(orderId)
                .idempotencyKey("paid-auto-" + orderId)
                .status(ShipmentStatus.CREATED.name())
                .shipFrom(null)
                .shipTo(toJsonOrNull(buildShipTo(order)))
                .declaredValue(order.getTotalAmount())
                .currency(order.getCurrency())
                .customsInfo(toJsonOrNull(CustomsInfoSnapshot.empty().getExtra()))
                .build();
    }

    /**
     * 构建订单构造收货地址
     *
     * @param order 订单行
     * @return 收货地址快照
     */
    private @NotNull ShippingAddressSnapshot buildShipTo(@NotNull OrdersPO order) {
        try {
            AddressSnapshot snapshot = objectMapper.readValue(order.getAddressSnapshot(), AddressSnapshot.class);
            String phone = "+" + snapshot.getPhoneCountryCode() + snapshot.getPhoneNationalNumber();
            return ShippingAddressSnapshot.of(
                    snapshot.getReceiverName(),
                    phone,
                    snapshot.getCountry(),
                    snapshot.getProvince(),
                    snapshot.getCity(),
                    snapshot.getDistrict(),
                    snapshot.getAddressLine1(),
                    snapshot.getAddressLine2(),
                    snapshot.getZipcode()
            );
        } catch (Exception exception) {
            throw new ConflictException("订单地址快照解析失败, " + exception.getMessage());
        }
    }

    /**
     * 构建订单发货地址
     *
     * @param userAddressPO 管理员选择的发货地址对象
     * @return 发货地址快照
     */
    private @NotNull ShippingAddressSnapshot buildShipFrom(@NotNull UserAddressPO userAddressPO) {
        String phone = "+" + userAddressPO.getPhoneCountryCode() + userAddressPO.getPhoneNationalNumber();
        return ShippingAddressSnapshot.of(
                userAddressPO.getReceiverName(),
                phone,
                userAddressPO.getCountry(),
                userAddressPO.getProvince(),
                userAddressPO.getCity(),
                userAddressPO.getDistrict(),
                userAddressPO.getAddressLine1(),
                userAddressPO.getAddressLine2(),
                userAddressPO.getZipcode()
        );
    }

    /**
     * 持久化对象转换为物流明细实体
     *
     * @param po 持久化对象
     * @return 实体
     */
    private @NotNull ShipmentItem toShipmentItemEntity(@NotNull ShipmentItemPO po) {
        return ShipmentItem.reconstitute(
                po.getId(),
                po.getShipmentId(),
                po.getOrderId(),
                po.getOrderItemId(),
                po.getProductId(),
                po.getSkuId(),
                po.getQuantity(),
                po.getCreatedAt()
        );
    }

    /**
     * 订单明细持久化对象转换为物流明细实体
     *
     * @param po 订单明细行
     * @return 物流明细实体
     */
    private @NotNull ShipmentItem toShipmentItemEntity(@NotNull OrderItemPO po) {
        return ShipmentItem.create(
                po.getOrderId(),
                po.getId(),
                po.getProductId(),
                po.getSkuId(),
                po.getQuantity()
        );
    }

    /**
     * 物流明细实体转换为持久化对象
     *
     * @param item 物流明细实体
     * @return 持久化对象
     */
    private @NotNull ShipmentItemPO toShipmentItemPO(@NotNull ShipmentItem item) {
        return ShipmentItemPO.builder()
                .id(item.getId())
                .shipmentId(item.getShipmentId())
                .orderId(item.getOrderId())
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .skuId(item.getSkuId())
                .quantity(item.getQuantity())
                .createdAt(item.getCreatedAt())
                .build();
    }

    /**
     * 状态日志持久化对象转换为实体
     *
     * @param po 持久化对象
     * @return 实体
     */
    private @NotNull ShipmentStatusLog toStatusLogEntity(@NotNull ShipmentStatusLogPO po) {
        return ShipmentStatusLog.reconstitute(
                po.getId(),
                po.getShipmentId(),
                po.getFromStatus() == null ? null : ShipmentStatus.valueOf(po.getFromStatus()),
                ShipmentStatus.valueOf(po.getToStatus()),
                po.getEventTime(),
                ShipmentStatusEventSource.valueOf(po.getSourceType()),
                po.getSourceRef(),
                po.getCarrierCode(),
                po.getTrackingNo(),
                po.getNote(),
                parseRawPayload(po.getRawPayload()),
                po.getRawPayload(),
                po.getActorUserId(),
                po.getCreatedAt()
        );
    }

    /**
     * 状态日志实体转换为持久化对象
     *
     * @param log 状态日志实体
     * @return 持久化对象
     */
    private @NotNull ShipmentStatusLogPO toStatusLogPO(@NotNull ShipmentStatusLog log) {
        String rawPayload = log.getRawPayloadText() == null
                ? toJsonOrNull(log.getRawPayload())
                : log.getRawPayloadText();
        return ShipmentStatusLogPO.builder()
                .id(log.getId())
                .shipmentId(log.getShipmentId())
                .fromStatus(log.getFromStatus() == null ? null : log.getFromStatus().name())
                .toStatus(log.getToStatus().name())
                .eventTime(log.getEventTime())
                .sourceType(log.getSourceType().name())
                .sourceRef(log.getSourceRef())
                .carrierCode(log.getCarrierCode())
                .trackingNo(log.getTrackingNo())
                .note(log.getNote())
                .rawPayload(rawPayload)
                .actorUserId(log.getActorUserId())
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * 物流聚合转换为持久化对象
     *
     * @param shipment 物流聚合
     * @return 持久化对象
     */
    private @NotNull ShipmentPO toShipmentPO(@NotNull Shipment shipment) {
        ShipmentDimension dimension = shipment.getDimension();
        return ShipmentPO.builder()
                .id(shipment.getId())
                .shipmentNo(shipment.getShipmentNo().getValue())
                .orderId(shipment.getOrderId())
                .idempotencyKey(shipment.getIdempotencyKey())
                .carrierCode(shipment.getCarrierCode())
                .carrierName(shipment.getCarrierName())
                .serviceCode(shipment.getServiceCode())
                .trackingNo(shipment.getTrackingNo())
                .extExternalId(shipment.getExtExternalId())
                .status(shipment.getStatus().name())
                .shipFrom(toJsonOrNull(shipment.getShipFrom()))
                .shipTo(toJsonOrNull(shipment.getShipTo()))
                .weightKg(dimension == null ? null : dimension.getWeightKg())
                .lengthCm(dimension == null ? null : dimension.getLengthCm())
                .widthCm(dimension == null ? null : dimension.getWidthCm())
                .heightCm(dimension == null ? null : dimension.getHeightCm())
                .declaredValue(shipment.getDeclaredValue())
                .currency(shipment.getCurrency())
                .customsInfo(toJsonOrNull(shipment.getCustomsInfo() == null ? null : shipment.getCustomsInfo().getExtra()))
                .labelUrl(shipment.getLabelUrl())
                .pickupTime(shipment.getPickupTime())
                .deliveredTime(shipment.getDeliveredTime())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    /**
     * 解析地址快照 JSON
     *
     * @param json JSON 文本
     * @return 地址快照
     */
    private @Nullable ShippingAddressSnapshot parseShippingAddress(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {
            });
            String receiverName = readText(raw, "receiverName", "receiver_name");
            String phone = readText(raw, "phone");
            String country = readText(raw, "country");
            String province = readText(raw, "province");
            String city = readText(raw, "city");
            String district = readText(raw, "district");
            String addressLine1 = readText(raw, "addressLine1", "address_line1");
            String addressLine2 = readText(raw, "addressLine2", "address_line2");
            String zipcode = readText(raw, "zipcode", "zip_code", "postal_code");
            return ShippingAddressSnapshot.of(
                    receiverName,
                    phone,
                    country,
                    province,
                    city,
                    district,
                    addressLine1,
                    addressLine2,
                    zipcode
            );
        } catch (Exception exception) {
            throw new ConflictException("物流地址快照解析失败, " + exception.getMessage());
        }
    }

    /**
     * 解析关务信息 JSON
     *
     * @param json JSON 文本
     * @return 关务快照
     */
    private @Nullable CustomsInfoSnapshot parseCustomsInfo(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {
            });
            return CustomsInfoSnapshot.of(raw);
        } catch (Exception exception) {
            throw new ConflictException("关务信息解析失败, " + exception.getMessage());
        }
    }

    /**
     * 解析状态日志原始报文
     *
     * @param json JSON 文本
     * @return Map 结构
     */
    private @Nullable Map<String, Object> parseRawPayload(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 将对象序列化为 JSON
     *
     * @param value 对象
     * @return JSON 文本
     */
    private @Nullable String toJsonOrNull(@Nullable Object value) {
        if (value == null)
            return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 从 Map 中读取文本字段
     *
     * @param raw  数据映射
     * @param keys 候选字段名
     * @return 文本值
     */
    private @Nullable String readText(@NotNull Map<String, Object> raw,
                                      @NotNull String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value == null)
                continue;
            String text = String.valueOf(value);
            if (!text.isBlank())
                return text.strip();
        }
        return null;
    }
}
