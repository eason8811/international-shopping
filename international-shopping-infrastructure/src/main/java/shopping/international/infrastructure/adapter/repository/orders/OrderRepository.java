package shopping.international.infrastructure.adapter.repository.orders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.domain.service.orders.IOrderService;
import shopping.international.infrastructure.dao.orders.*;
import shopping.international.infrastructure.dao.orders.po.*;
import shopping.international.infrastructure.dao.products.ProductSkuMapper;
import shopping.international.infrastructure.dao.products.po.ProductSkuPO;
import shopping.international.types.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 基于 MyBatis-Plus 的订单聚合仓储实现
 *
 * <p>职责:</p>
 * <ul>
 *     <li>组合读写 {@code orders + order_item}</li>
 *     <li>落库订单状态流转日志 {@code order_status_log}</li>
 *     <li>落库库存变动日志 {@code inventory_log} 并原子调整 {@code product_sku.stock}</li>
 *     <li>管理侧只读查询 (列表/日志)</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class OrderRepository implements IOrderRepository {

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
     * inventory_log Mapper
     */
    private final InventoryLogMapper inventoryLogMapper;
    /**
     * shopping_cart_item Mapper (用于下单后清理购物车)
     */
    private final ShoppingCartItemMapper shoppingCartItemMapper;
    /**
     * order_discount_applied Mapper (用于记录折扣实际应用流水)
     */
    private final OrderDiscountAppliedMapper orderDiscountAppliedMapper;
    /**
     * product_sku Mapper (用于原子扣减/回补库存)
     */
    private final ProductSkuMapper productSkuMapper;
    /**
     * JSON 序列化/反序列化工具
     */
    private final ObjectMapper objectMapper;

    // ========================= 用户侧查询 =========================

    /**
     * 查询用户侧订单摘要列表
     *
     * @param userId      用户 ID
     * @param status      状态过滤
     * @param createdFrom 创建时间起
     * @param createdTo   创建时间止
     * @param offset      偏移量
     * @param limit       单页数量
     * @return 摘要列表
     */
    @Override
    public @NotNull List<IOrderService.OrderSummaryRow> pageUserOrderSummaries(@NotNull Long userId,
                                                                               @Nullable OrderStatus status,
                                                                               @Nullable LocalDateTime createdFrom,
                                                                               @Nullable LocalDateTime createdTo,
                                                                               int offset, int limit) {
        LambdaQueryWrapper<OrdersPO> wrapper = buildPageOrderSummariesWrapper(userId, status, createdFrom, createdTo);
        wrapper.orderByDesc(OrdersPO::getCreatedAt)
                .last("limit " + limit + " offset " + offset);

        List<OrdersPO> pos = ordersMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream()
                .map(po -> new IOrderService.OrderSummaryRow(
                        po.getOrderNo(),
                        OrderStatus.valueOf(po.getStatus()),
                        po.getItemsCount(),
                        nullToZero(po.getTotalAmount()),
                        nullToZero(po.getDiscountAmount()),
                        nullToZero(po.getShippingAmount()),
                        nullToZero(po.getTaxAmount()),
                        nullToZero(po.getPayAmount()),
                        po.getCurrency(),
                        PayChannel.valueOf(po.getPayChannel()),
                        PayStatus.valueOf(po.getPayStatus()),
                        po.getPayTime(),
                        po.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 统计用户侧订单摘要数量
     *
     * @param userId      用户 ID
     * @param status      状态过滤
     * @param createdFrom 创建时间起
     * @param createdTo   创建时间止
     * @return 总数
     */
    @Override
    public long countUserOrderSummaries(@NotNull Long userId,
                                        @Nullable OrderStatus status,
                                        @Nullable LocalDateTime createdFrom,
                                        @Nullable LocalDateTime createdTo) {
        LambdaQueryWrapper<OrdersPO> wrapper = buildPageOrderSummariesWrapper(userId, status, createdFrom, createdTo);
        return ordersMapper.selectCount(wrapper);
    }

    /**
     * 查询用户侧订单详情 (含明细)
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return Optional
     */
    @Override
    public @NotNull Optional<Order> findUserOrderDetail(@NotNull Long userId, @NotNull OrderNo orderNo) {
        OrdersPO orderPo = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getOrderNo, orderNo.getValue())
                .eq(OrdersPO::getUserId, userId)
                .last("limit 1"));
        if (orderPo == null)
            return Optional.empty();
        return Optional.of(assembleOrder(orderPo));
    }

    // ========================= 管理侧查询 =========================

    /**
     * 分页查询管理侧订单列表
     *
     * @param criteria 查询条件
     * @param offset   偏移量
     * @param limit    单页数量
     * @return 列表行
     */
    @Override
    public @NotNull List<IAdminOrderService.AdminOrderListItemView> pageAdminOrders(@NotNull AdminOrderSearchCriteria criteria, int offset, int limit) {
        LambdaQueryWrapper<OrdersPO> wrapper = buildPageAdminOrdersWrapper(criteria);
        wrapper.orderByDesc(OrdersPO::getCreatedAt)
                .last("limit " + limit + " offset " + offset);
        List<OrdersPO> pos = ordersMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream()
                .map(po -> new IAdminOrderService.AdminOrderListItemView(
                        po.getId(),
                        po.getOrderNo(),
                        po.getUserId(),
                        OrderStatus.valueOf(po.getStatus()),
                        po.getItemsCount(),
                        nullToZero(po.getTotalAmount()),
                        nullToZero(po.getDiscountAmount()),
                        nullToZero(po.getShippingAmount()),
                        nullToZero(po.getTaxAmount()),
                        nullToZero(po.getPayAmount()),
                        po.getCurrency(),
                        PayChannel.valueOf(po.getPayChannel()),
                        PayStatus.valueOf(po.getPayStatus()),
                        po.getPaymentExternalId(),
                        po.getPayTime(),
                        po.getCreatedAt(),
                        po.getUpdatedAt()
                ))
                .toList();
    }

    /**
     * 统计管理侧订单数量
     *
     * @param criteria 查询条件
     * @return 总数
     */
    @Override
    public long countAdminOrders(@NotNull AdminOrderSearchCriteria criteria) {
        LambdaQueryWrapper<OrdersPO> wrapper = buildPageAdminOrdersWrapper(criteria);
        return ordersMapper.selectCount(wrapper);
    }

    /**
     * 查询管理侧订单详情 (含明细)
     *
     * @param orderNo 订单号
     * @return Optional
     */
    @Override
    public @NotNull Optional<Order> findOrderDetail(@NotNull OrderNo orderNo) {
        OrdersPO orderPo = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getOrderNo, orderNo.getValue())
                .last("limit 1"));
        if (orderPo == null)
            return Optional.empty();
        return Optional.of(assembleOrder(orderPo));
    }

    /**
     * 查询待支付且已超时的订单
     *
     * @param createdBefore 创建时间上限
     * @param limit         最大返回数量
     * @return 订单列表
     */
    @Override
    public @NotNull List<Order> listTimeoutCandidates(@NotNull LocalDateTime createdBefore, int limit) {
        int safeLimit = Math.max(limit, 1);
        LambdaQueryWrapper<OrdersPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrdersPO::getStatus, OrderStatus.CREATED.name(), OrderStatus.PENDING_PAYMENT.name())
                .ne(OrdersPO::getPayStatus, PayStatus.SUCCESS.name())
                .le(OrdersPO::getCreatedAt, createdBefore)
                .orderByAsc(OrdersPO::getCreatedAt)
                .last("limit " + safeLimit);
        List<OrdersPO> pos = ordersMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(this::assembleOrder).toList();
    }

    /**
     * 根据用户 id 和幂等性键查找订单信息
     *
     * @param userId         用户的唯一标识符 不能为 null
     * @param idempotencyKey 幂等性键 用于确保操作的幂等性 不能为 null
     * @return 如果找到匹配的订单, 则返回包含该订单信息的 {@link Optional} 对象; 否则返回空的 {@link Optional}
     * <p>此方法通过提供的用户 id 和幂等性键来查询数据库中的订单记录 如果没有找到相应的订单, 将返回一个空的 {@code Optional} 对象</p>
     */
    private @NotNull Optional<Order> findOrderByIdempotencyKey(@NotNull Long userId, @NotNull String idempotencyKey) {
        OrdersPO orderPo = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getUserId, userId)
                .eq(OrdersPO::getIdempotencyKey, idempotencyKey)
                .last("limit 1"));
        if (orderPo == null)
            return Optional.empty();
        return Optional.of(assembleOrder(orderPo));
    }

    // ========================= 审计查询 =========================

    /**
     * 查询订单状态流转日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @Override
    public @NotNull List<OrderStatusLog> listStatusLogs(@NotNull OrderNo orderNo) {
        OrdersPO orderPo = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .select(OrdersPO::getId)
                .eq(OrdersPO::getOrderNo, orderNo.getValue())
                .last("limit 1"));
        if (orderPo == null)
            return List.of();
        List<OrderStatusLogPO> logs = orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLogPO>()
                .eq(OrderStatusLogPO::getOrderId, orderPo.getId())
                .orderByAsc(OrderStatusLogPO::getId));
        if (logs == null || logs.isEmpty())
            return List.of();
        return logs.stream()
                .map(po -> new OrderStatusLog(
                        po.getId(),
                        po.getOrderId(),
                        OrderStatusEventSource.valueOf(po.getEventSource()),
                        po.getFromStatus() == null ? null : OrderStatus.valueOf(po.getFromStatus()),
                        OrderStatus.valueOf(po.getToStatus()),
                        po.getNote(),
                        po.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 查询订单关联库存日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @Override
    public @NotNull List<InventoryLog> listInventoryLogsByOrderNo(@NotNull OrderNo orderNo) {
        OrdersPO orderPo = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .select(OrdersPO::getId)
                .eq(OrdersPO::getOrderNo, orderNo.getValue())
                .last("limit 1"));
        if (orderPo == null)
            return List.of();
        List<InventoryLogPO> logs = inventoryLogMapper.selectList(new LambdaQueryWrapper<InventoryLogPO>()
                .eq(InventoryLogPO::getOrderId, orderPo.getId())
                .orderByAsc(InventoryLogPO::getId));
        if (logs == null || logs.isEmpty())
            return List.of();
        return logs.stream().map(po -> new InventoryLog(
                po.getId(),
                po.getSkuId(),
                po.getOrderId(),
                InventoryChangeType.valueOf(po.getChangeType()),
                po.getQuantity(),
                po.getReason(),
                po.getCreatedAt()
        )).toList();
    }

    /**
     * 分页查询库存日志
     *
     * @param criteria 条件
     * @param offset   偏移量
     * @param limit    单页数量
     * @return 日志列表
     */
    @Override
    public @NotNull List<InventoryLog> pageInventoryLogs(@NotNull InventoryLogSearchCriteria criteria, int offset, int limit) {
        LambdaQueryWrapper<InventoryLogPO> wrapper = buildPageInventoryLogsWrapper(criteria);
        wrapper.orderByDesc(InventoryLogPO::getCreatedAt)
                .orderByDesc(InventoryLogPO::getId)
                .last("limit " + limit + " offset " + offset);
        List<InventoryLogPO> pos = inventoryLogMapper.selectList(wrapper);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(po -> new InventoryLog(
                po.getId(),
                po.getSkuId(),
                po.getOrderId(),
                InventoryChangeType.valueOf(po.getChangeType()),
                po.getQuantity(),
                po.getReason(),
                po.getCreatedAt()
        )).toList();
    }

    /**
     * 统计库存日志数量
     *
     * @param criteria 条件
     * @return 总数
     */
    @Override
    public long countInventoryLogs(@NotNull InventoryLogSearchCriteria criteria) {
        LambdaQueryWrapper<InventoryLogPO> wrapper = buildPageInventoryLogsWrapper(criteria);
        return inventoryLogMapper.selectCount(wrapper);
    }

    // ========================= 写入 =========================

    /**
     * 创建订单并预占库存
     *
     * @param order                  订单聚合
     * @param eventSource            状态日志来源
     * @param note                   状态日志备注
     * @param cartItemIdsToDelete    需要删除的购物车条目 ID 列表
     * @param discountAppliedCreates 折扣流水写入参数
     * @return 保存后的订单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order createOrderAndReserveStock(@NotNull Order order,
                                                     @NotNull OrderStatusEventSource eventSource,
                                                     @Nullable String note,
                                                     @Nullable List<Long> cartItemIdsToDelete,
                                                     @Nullable List<IOrderService.OrderDiscountApplied> discountAppliedCreates,
                                                     @Nullable String idempotencyKey) {
        // 0) 幂等键命中直接回读
        if (idempotencyKey != null) {
            Optional<Order> existing = findOrderByIdempotencyKey(order.getUserId(), idempotencyKey);
            if (existing.isPresent())
                return existing.get();
        }

        // 1) 插入 orders
        OrdersPO ordersPO = toOrdersPO(order, idempotencyKey);
        try {
            ordersMapper.insert(ordersPO);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey != null)
                return findOrderByIdempotencyKey(order.getUserId(), idempotencyKey)
                        .orElseThrow(() -> new ConflictException("订单幂等键已存在但回读失败", e));
            throw e;
        }
        Long orderId = ordersPO.getId();

        // 2) 插入 order_item
        List<OrderItemPO> orderItemPOList = order.getItems().stream()
                .map(item -> toOrderItemPO(orderId, item))
                .toList();
        orderItemMapper.insert(orderItemPOList);
        Map<Long, Long> skuIdToOrderItemId = orderItemPOList.stream()
                .collect(Collectors.toMap(OrderItemPO::getSkuId, OrderItemPO::getId));

        // 3) 原子扣减库存 + 写 inventory_log (RESERVE)
        reserveStockAndWriteInventoryLogs(orderId, order.getItems(), InventoryChangeType.RESERVE, "下单预占");

        // 4) 写 order_status_log (CREATE)
        insertStatusLog(orderId, eventSource, null, order.getStatus(), note);

        // 5) 清理购物车勾选条目
        if (cartItemIdsToDelete != null && !cartItemIdsToDelete.isEmpty())
            shoppingCartItemMapper.delete(new LambdaQueryWrapper<ShoppingCartItemPO>()
                    .eq(ShoppingCartItemPO::getUserId, order.getUserId())
                    .in(ShoppingCartItemPO::getId, cartItemIdsToDelete));

        // 6) 写入折扣实际应用流水
        if (discountAppliedCreates != null && !discountAppliedCreates.isEmpty())
            insertDiscountApplied(orderId, order.getCurrency(), skuIdToOrderItemId, discountAppliedCreates);

        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单创建后回读失败"));
    }

    /**
     * 更新订单收货地址快照
     *
     * @param order           订单聚合
     * @param addressSnapshot 新的地址快照
     * @return 更新后的订单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order updateAddressSnapshot(@NotNull Order order, @NotNull AddressSnapshot addressSnapshot) {
        String json = toJsonOrNull(addressSnapshot);
        LambdaUpdateWrapper<OrdersPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrdersPO::getId, order.getId())
                .eq(OrdersPO::getStatus, order.getStatus().name())
                .eq(OrdersPO::getAddressChanged, Boolean.FALSE)
                .set(OrdersPO::getAddressSnapshot, json)
                .set(OrdersPO::getAddressChanged, Boolean.TRUE);
        int updated = ordersMapper.update(null, wrapper);
        if (updated <= 0)
            throw new ConflictException("订单状态已变更或已修改过地址, 无法修改地址");
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单更新后回读失败"));
    }

    /**
     * 取消订单并回补库存
     *
     * @param order       订单聚合 (已变更为 CANCELLED)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        备注
     * @return 更新后的订单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order cancelAndReleaseStock(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                                @NotNull OrderStatusEventSource eventSource, @Nullable String note) {
        updateOrderByStatusOrThrow(order, fromStatus, "取消", wrapper -> wrapper
                .ne(OrdersPO::getPayStatus, PayStatus.SUCCESS.name())
                .and(w ->
                        w.eq(OrdersPO::getStatus, OrderStatus.CREATED.name()).or().eq(OrdersPO::getStatus, OrderStatus.PENDING_PAYMENT)
                )
                .set(OrdersPO::getStatus, order.getStatus().name())
                .set(OrdersPO::getCancelReason, order.getCancelReason() == null ? null : order.getCancelReason().getValue())
                .set(OrdersPO::getCancelTime, order.getCancelTime())
                .set(OrdersPO::getPayStatus, order.getPayStatus() == null ? PayStatus.NONE.name() : order.getPayStatus().name())
        );
        insertStatusLog(order.getId(), eventSource, fromStatus, order.getStatus(), note);
        reserveStockAndWriteInventoryLogs(order.getId(), order.getItems(), InventoryChangeType.RELEASE, note);
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单取消后回读失败"));
    }

    /**
     * 发起退款申请
     *
     * @param order       订单聚合 (已变更为 REFUNDING)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        备注
     * @return 更新后的订单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order requestRefund(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                        @NotNull OrderStatusEventSource eventSource, @Nullable String note) {
        updateOrderByStatusOrThrow(order, fromStatus, "申请退款", wrapper -> wrapper
                .set(OrdersPO::getStatus, order.getStatus().name())
                .set(OrdersPO::getRefundReasonSnapshot, toJsonOrNull(order.getLastRefundReason()))
        );
        insertStatusLog(order.getId(), eventSource, fromStatus, order.getStatus(), note);
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单退款申请后回读失败"));
    }

    /**
     * 确认退款并回补库存
     *
     * @param order       订单聚合 (已变更为 REFUNDED)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        备注
     * @return 更新后的订单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order confirmRefundAndRestock(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                                  @NotNull OrderStatusEventSource eventSource, @Nullable String note) {
        updateOrderByStatusOrThrow(order, fromStatus, "确认退款", wrapper -> wrapper
                .set(OrdersPO::getStatus, order.getStatus().name())
        );
        insertStatusLog(order.getId(), eventSource, fromStatus, order.getStatus(), note);
        reserveStockAndWriteInventoryLogs(order.getId(), order.getItems(), InventoryChangeType.RESTOCK, note);
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单确认退款后回读失败"));
    }

    /**
     * 关闭订单
     *
     * @param order       订单聚合 (已变更为 CLOSED)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        备注
     * @return 更新后的订单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order close(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                @NotNull OrderStatusEventSource eventSource, @Nullable String note) {
        updateOrderByStatusOrThrow(order, fromStatus, "关闭", wrapper -> wrapper
                .set(OrdersPO::getStatus, order.getStatus().name())
        );
        insertStatusLog(order.getId(), eventSource, fromStatus, order.getStatus(), note);
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单关闭后回读失败"));
    }

    // ========================= 内部装配与落库辅助 =========================

    /**
     * 构建用于分页查询订单摘要信息的 <code>LambdaQueryWrapper</code> 对象
     *
     * @param userId      用户 ID 必填项, 用于筛选属于该用户的订单
     * @param status      订单状态 可选项, 如果不为空, 则使用此参数过滤具有指定状态的订单
     * @param createdFrom 创建时间起始点 可选项, 如果提供, 将只包括在此时间之后创建的订单
     * @param createdTo   创建时间结束点 可选项, 如果提供, 将只包括在此时间之前创建的订单
     * @return 返回一个配置好的 <code>LambdaQueryWrapper<OrdersPO></code> 实例, 该实例可用于进一步的数据检索操作
     */
    private LambdaQueryWrapper<OrdersPO> buildPageOrderSummariesWrapper(@NotNull Long userId, @Nullable OrderStatus status, @Nullable LocalDateTime createdFrom, @Nullable LocalDateTime createdTo) {
        LambdaQueryWrapper<OrdersPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrdersPO::getUserId, userId);
        if (status != null)
            wrapper.eq(OrdersPO::getStatus, status.name());
        if (createdFrom != null)
            wrapper.ge(OrdersPO::getCreatedAt, createdFrom);
        if (createdTo != null)
            wrapper.le(OrdersPO::getCreatedAt, createdTo);
        return wrapper;
    }

    /**
     * 构建用于管理员查询订单的 <code>LambdaQueryWrapper</code> 对象
     *
     * @param criteria 查询条件, 包括订单号, 用户 ID, 订单状态, 支付状态, 支付渠道, 外部支付 ID, 货币类型, 创建时间范围等
     *                 该参数不能为空
     * @return 返回构建好的 <code>LambdaQueryWrapper<OrdersPO></code> 实例, 可以直接用于 MyBatis-Plus 的查询操作
     */
    private LambdaQueryWrapper<OrdersPO> buildPageAdminOrdersWrapper(@NotNull AdminOrderSearchCriteria criteria) {
        LambdaQueryWrapper<OrdersPO> wrapper = new LambdaQueryWrapper<>();
        if (criteria.getOrderNo() != null && !criteria.getOrderNo().isBlank())
            wrapper.eq(OrdersPO::getOrderNo, criteria.getOrderNo().strip());
        if (criteria.getUserId() != null)
            wrapper.eq(OrdersPO::getUserId, criteria.getUserId());
        if (criteria.getStatus() != null)
            wrapper.eq(OrdersPO::getStatus, criteria.getStatus().name());
        if (criteria.getPayStatus() != null)
            wrapper.eq(OrdersPO::getPayStatus, criteria.getPayStatus().name());
        if (criteria.getPayChannel() != null)
            wrapper.eq(OrdersPO::getPayChannel, criteria.getPayChannel().name());
        if (criteria.getPaymentExternalId() != null && !criteria.getPaymentExternalId().isBlank())
            wrapper.eq(OrdersPO::getPaymentExternalId, criteria.getPaymentExternalId().strip());
        if (criteria.getCurrency() != null && !criteria.getCurrency().isBlank())
            wrapper.eq(OrdersPO::getCurrency, criteria.getCurrency().strip());
        if (criteria.getCreatedFrom() != null)
            wrapper.ge(OrdersPO::getCreatedAt, criteria.getCreatedFrom());
        if (criteria.getCreatedTo() != null)
            wrapper.le(OrdersPO::getCreatedAt, criteria.getCreatedTo());
        return wrapper;
    }

    /**
     * 构建用于分页查询库存日志的 <code>LambdaQueryWrapper</code>
     * 根据传入的搜索条件 {@link InventoryLogSearchCriteria} 设置相应的查询条件
     *
     * @param criteria 包含查询条件的对象, 不能为 null
     * @return 返回一个配置好的 <code>LambdaQueryWrapper<InventoryLogPO></code> 对象, 用于后续的数据库查询操作
     */
    private LambdaQueryWrapper<InventoryLogPO> buildPageInventoryLogsWrapper(@NotNull InventoryLogSearchCriteria criteria) {
        LambdaQueryWrapper<InventoryLogPO> wrapper = new LambdaQueryWrapper<>();
        if (criteria.getChangeType() != null)
            wrapper.eq(InventoryLogPO::getChangeType, criteria.getChangeType().name());
        if (criteria.getSkuId() != null)
            wrapper.eq(InventoryLogPO::getSkuId, criteria.getSkuId());
        if (criteria.getOrderId() != null)
            wrapper.eq(InventoryLogPO::getOrderId, criteria.getOrderId());
        if (criteria.getQuantityMin() != null)
            wrapper.ge(InventoryLogPO::getQuantity, criteria.getQuantityMin());
        if (criteria.getQuantityMax() != null)
            wrapper.le(InventoryLogPO::getQuantity, criteria.getQuantityMax());
        if (criteria.getCreatedFrom() != null)
            wrapper.ge(InventoryLogPO::getCreatedAt, criteria.getCreatedFrom());
        if (criteria.getCreatedTo() != null)
            wrapper.le(InventoryLogPO::getCreatedAt, criteria.getCreatedTo());
        return wrapper;
    }

    /**
     * 装配订单聚合 (含明细)
     *
     * @param orderPo orders 持久化对象
     * @return 订单聚合
     */
    private Order assembleOrder(OrdersPO orderPo) {
        Long orderId = orderPo.getId();
        List<OrderItemPO> itemPos = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getOrderId, orderId)
                .orderByAsc(OrderItemPO::getId));
        List<OrderItem> items = (itemPos == null || itemPos.isEmpty()) ? List.of() : itemPos.stream()
                .map(po -> toEntityOrderItem(orderPo.getCurrency(), po))
                .toList();

        OrderNo orderNo = OrderNo.of(orderPo.getOrderNo());
        boolean addressChanged = Boolean.TRUE.equals(orderPo.getAddressChanged());

        return Order.reconstitute(
                orderPo.getId(),
                orderNo,
                orderPo.getUserId(),
                OrderStatus.valueOf(orderPo.getStatus()),
                Money.ofMinor(orderPo.getCurrency(), nullToZero(orderPo.getTotalAmount())),
                Money.ofMinor(orderPo.getCurrency(), nullToZero(orderPo.getDiscountAmount())),
                Money.ofMinor(orderPo.getCurrency(), nullToZero(orderPo.getShippingAmount())),
                Money.ofMinor(orderPo.getCurrency(), nullToZero(orderPo.getTaxAmount())),
                Money.ofMinor(orderPo.getCurrency(), nullToZero(orderPo.getPayAmount())),
                orderPo.getCurrency(),
                PayChannel.valueOf(orderPo.getPayChannel()),
                PayStatus.valueOf(orderPo.getPayStatus()),
                orderPo.getPaymentExternalId(),
                orderPo.getPayTime(),
                parseAddressSnapshot(orderPo.getAddressSnapshot()),
                BuyerRemark.ofNullable(orderPo.getBuyerRemark()),
                orderPo.getCancelReason() == null ? null : CancelReason.of(orderPo.getCancelReason()),
                orderPo.getCancelTime(),
                addressChanged,
                parseRefundReason(orderPo.getRefundReasonSnapshot()),
                items,
                orderPo.getCreatedAt(),
                orderPo.getUpdatedAt()
        );
    }

    /**
     * orders 聚合 → OrdersPO
     *
     * @param order 订单聚合
     * @return 持久化对象
     */
    private OrdersPO toOrdersPO(Order order, @Nullable String idempotencyKey) {
        return OrdersPO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo().getValue())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .itemsCount(order.getItemsCount())
                .totalAmount(order.getTotalAmount().getAmountMinor())
                .discountAmount(order.getDiscountAmount().getAmountMinor())
                .shippingAmount(order.getShippingAmount().getAmountMinor())
                .taxAmount(order.getTaxAmount().getAmountMinor())
                .payAmount(order.getPayAmount().getAmountMinor())
                .currency(order.getCurrency())
                .payChannel(order.getPayChannel() == null ? PayChannel.NONE.name() : order.getPayChannel().name())
                .payStatus(order.getPayStatus() == null ? PayStatus.NONE.name() : order.getPayStatus().name())
                .paymentExternalId(order.getPaymentExternalId())
                .payTime(order.getPayTime())
                .addressSnapshot(toJsonOrNull(order.getAddressSnapshot()))
                .addressChanged(order.isAddressChanged())
                .buyerRemark(order.getBuyerRemark() == null ? null : order.getBuyerRemark().getValue())
                .idempotencyKey(idempotencyKey)
                .cancelReason(order.getCancelReason() == null ? null : order.getCancelReason().getValue())
                .cancelTime(order.getCancelTime())
                .refundReasonSnapshot(toJsonOrNull(order.getLastRefundReason()))
                .build();
    }

    /**
     * OrderItem → OrderItemPO
     *
     * @param orderId 订单 ID
     * @param item    明细实体
     * @return 持久化对象
     */
    private OrderItemPO toOrderItemPO(Long orderId, OrderItem item) {
        return OrderItemPO.builder()
                .orderId(orderId)
                .productId(item.getProductId())
                .skuId(item.getSkuId())
                .discountCodeId(item.getDiscountCodeId())
                .title(item.getTitle())
                .skuAttrs(toJsonOrNull(item.getSkuAttrs()))
                .coverImageUrl(item.getCoverImageUrl())
                .unitPrice(item.getUnitPrice().getAmountMinor())
                .quantity(item.getQuantity())
                .subtotalAmount(item.getSubtotalAmount().getAmountMinor())
                .build();
    }

    /**
     * OrderItemPO → OrderItem 实体
     *
     * @param currency 订单币种
     * @param po       持久化对象
     * @return 领域实体
     */
    private OrderItem toEntityOrderItem(String currency, OrderItemPO po) {
        return OrderItem.reconstitute(
                po.getId(),
                po.getOrderId(),
                po.getProductId(),
                po.getSkuId(),
                po.getDiscountCodeId(),
                po.getTitle(),
                parseMap(po.getSkuAttrs()),
                po.getCoverImageUrl(),
                Money.ofMinor(currency, nullToZero(po.getUnitPrice())),
                po.getQuantity() == null ? 0 : po.getQuantity(),
                Money.ofMinor(currency, nullToZero(po.getSubtotalAmount())),
                po.getCreatedAt()
        );
    }

    /**
     * 写入状态流转日志
     *
     * @param orderId    订单 ID
     * @param source     事件来源
     * @param fromStatus 源状态 (可为空)
     * @param toStatus   目标状态
     * @param note       备注 (可为空)
     */
    private void insertStatusLog(Long orderId, OrderStatusEventSource source, @Nullable OrderStatus fromStatus,
                                 OrderStatus toStatus, @Nullable String note) {
        String safeNote = note == null ? null : (note.length() <= 255 ? note : note.substring(0, 255));
        OrderStatusLogPO log = OrderStatusLogPO.builder()
                .orderId(orderId)
                .eventSource(source.name())
                .fromStatus(fromStatus == null ? null : fromStatus.name())
                .toStatus(toStatus.name())
                .note(safeNote)
                .build();
        orderStatusLogMapper.insert(log);
    }

    /**
     * 预占/回补库存并写入库存日志
     *
     * @param orderId    订单 ID
     * @param items      订单明细列表
     * @param changeType 变更类型
     * @param reason     原因备注
     */
    private void reserveStockAndWriteInventoryLogs(Long orderId, List<OrderItem> items, InventoryChangeType changeType, @Nullable String reason) {
        // 为避免死锁, 按 skuId 排序后逐条更新
        List<OrderItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(OrderItem::getSkuId));
        for (OrderItem item : sorted) {
            int qty = item.getQuantity();
            boolean inserted = tryInsertInventoryLog(orderId, item.getSkuId(), changeType, qty, reason);
            if (!inserted)
                continue;
            if (changeType == InventoryChangeType.RESERVE)
                decreaseStockOrThrow(item.getSkuId(), qty);
            else if (changeType == InventoryChangeType.RELEASE || changeType == InventoryChangeType.RESTOCK)
                increaseStock(item.getSkuId(), qty);
        }
    }

    /**
     * 尝试插入一条库存变更日志 如果存在联合唯一索引 {@code (order_id, sku_id, change_type)} 冲突则插入失败
     *
     * @param orderId    订单 id
     * @param skuId      商品 sku id
     * @param changeType 库存变更类型, 详见 {@link InventoryChangeType}
     * @param qty        变更数量
     * @param reason     变更原因, 最大长度为 255 字符, 超过会被截断
     * @return 如果成功插入返回 true, 否则返回 false
     */
    private boolean tryInsertInventoryLog(Long orderId, Long skuId, InventoryChangeType changeType, int qty, @Nullable String reason) {
        InventoryLogPO log = InventoryLogPO.builder()
                .skuId(skuId)
                .orderId(orderId)
                .changeType(changeType.name())
                .quantity(qty)
                .reason(reason == null ? null : (reason.length() <= 255 ? reason : reason.substring(0, 255)))
                .build();
        try {
            inventoryLogMapper.insert(log);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * 原子扣减 SKU 库存, 库存不足则抛出冲突异常
     *
     * @param skuId SKU ID
     * @param qty   扣减数量
     */
    private void decreaseStockOrThrow(Long skuId, int qty) {
        LambdaUpdateWrapper<ProductSkuPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductSkuPO::getId, skuId)
                .eq(ProductSkuPO::getStatus, "ENABLED")
                .inSql(ProductSkuPO::getProductId, "select id from product where status = 'ON_SALE'")
                .ge(ProductSkuPO::getStock, qty)
                .setSql("stock = stock - " + qty);
        int updated = productSkuMapper.update(null, wrapper);
        if (updated <= 0)
            throw new ConflictException("库存不足: skuId=" + skuId);
    }

    /**
     * 原子增加 SKU 库存
     *
     * @param skuId SKU ID
     * @param qty   增加数量
     */
    private void increaseStock(Long skuId, int qty) {
        LambdaUpdateWrapper<ProductSkuPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ProductSkuPO::getId, skuId)
                .setSql("stock = stock + " + qty);
        int updated = productSkuMapper.update(null, wrapper);
        if (updated <= 0)
            throw new ConflictException("库存回补失败: skuId=" + skuId);
    }

    /**
     * 根据指定的订单状态更新订单信息, 如果订单状态已变更, 则抛出异常
     *
     * @param order      待更新的 {@link Order} 对象
     * @param fromStatus 更新前的订单状态, 用于确认订单当前状态是否符合条件
     * @param action     尝试执行的操作名称, 用于在抛出异常时提供上下文信息
     * @param updater    {@link LambdaUpdateWrapper} 处理器
     * @throws ConflictException 当订单状态与预期不符, 导致无法执行更新操作时抛出
     */
    private void updateOrderByStatusOrThrow(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                            @NotNull String action,
                                            @NotNull Consumer<LambdaUpdateWrapper<OrdersPO>> updater) {
        LambdaUpdateWrapper<OrdersPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrdersPO::getId, order.getId())
                .eq(OrdersPO::getStatus, fromStatus.name());
        updater.accept(wrapper);
        int updated = ordersMapper.update(null, wrapper);
        if (updated <= 0)
            throw new ConflictException("订单状态已变更, 无法" + action);
    }

    /**
     * 写入折扣实际应用流水
     *
     * @param orderId            订单 ID
     * @param skuIdToOrderItemId skuId → order_item.id 的映射
     * @param appliedList        写入参数
     */
    private void insertDiscountApplied(Long orderId, String currency, Map<Long, Long> skuIdToOrderItemId, List<IOrderService.OrderDiscountApplied> appliedList) {
        requireNotNull(currency, "currency 不能为空");
        for (IOrderService.OrderDiscountApplied applied : appliedList) {
            requireNotNull(applied, "折扣应用参数不能为空");
            requireNotNull(applied.discountCodeId(), "折扣应用缺少 discountCodeId");
            requireNotNull(applied.appliedScope(), "折扣应用缺少 appliedScope");
            requireNotNull(applied.baseCurrency(), "折扣应用缺少 baseCurrency");

            Long orderItemId = null;
            if (applied.appliedScope() == DiscountApplyScope.ITEM) {
                requireNotNull(applied.skuId(), "明细级折扣缺少 skuId");
                orderItemId = skuIdToOrderItemId.get(applied.skuId());
                requireNotNull(orderItemId, "明细级折扣找不到对应 order_item");
            }

            OrderDiscountAppliedPO po = OrderDiscountAppliedPO.builder()
                    .orderId(orderId)
                    .orderItemId(orderItemId)
                    .discountCodeId(applied.discountCodeId())
                    .appliedScope(applied.appliedScope().name())
                    .currency(currency)
                    .appliedAmount(applied.appliedAmountMinor())
                    .baseCurrency(applied.baseCurrency())
                    .appliedAmountBase(applied.appliedAmountBaseMinor())
                    .fxRate(applied.fxRate())
                    .fxAsOf(applied.fxAsOf())
                    .fxProvider(applied.fxProvider() == null ? null : applied.fxProvider().name())
                    .build();
            orderDiscountAppliedMapper.insert(po);
        }
    }

    /**
     * 解析 address_snapshot JSON
     *
     * @param json JSON 文本
     * @return 地址快照或 null
     */
    private AddressSnapshot parseAddressSnapshot(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            return AddressSnapshot.of(
                    asString(map.get("receiverName")),
                    asString(map.get("phone")),
                    asNullableString(map.get("country")),
                    asNullableString(map.get("province")),
                    asNullableString(map.get("city")),
                    asNullableString(map.get("district")),
                    asString(map.get("addressLine1")),
                    asNullableString(map.get("addressLine2")),
                    asNullableString(map.get("zipcode"))
            );
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 解析 refund_reason_snapshot JSON
     *
     * @param json JSON 文本
     * @return 退款原因或 null
     */
    private OrderRefundReason parseRefundReason(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            String reasonCode = asNullableString(map.get("reasonCode"));
            if (reasonCode == null)
                return null;
            String reasonText = asNullableString(map.get("reasonText"));
            List<String> attachments = null;
            Object raw = map.get("attachments");
            if (raw instanceof List<?> list) {
                attachments = list.stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .map(String::strip)
                        .filter(s -> !s.isBlank())
                        .toList();
            }
            return OrderRefundReason.of(OrderRefundReasonCode.valueOf(reasonCode), reasonText, attachments);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 解析 JSON Map
     *
     * @param json JSON 文本
     * @return Map 或 null
     */
    private Map<String, Object> parseMap(@Nullable String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 对象序列化为 JSON, 失败返回 null
     *
     * @param obj 待序列化对象
     * @return JSON 字符串
     */
    private String toJsonOrNull(@Nullable Object obj) {
        if (obj == null)
            return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Long null → 0
     */
    private static long nullToZero(@Nullable Long v) {
        return v == null ? 0L : v;
    }

    /**
     * Object → 必填字符串
     *
     * @param value 值
     * @return 字符串
     */
    private static String asString(Object value) {
        if (value == null)
            return "";
        String s = String.valueOf(value);
        return s == null ? "" : s;
    }

    /**
     * Object → 可为空字符串
     *
     * @param value 值
     * @return 字符串或 null
     */
    private static String asNullableString(Object value) {
        if (value == null)
            return null;
        String s = String.valueOf(value);
        return s == null || s.isBlank() ? null : s;
    }
}
