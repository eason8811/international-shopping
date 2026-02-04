package shopping.international.infrastructure.adapter.repository.orders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.domain.model.enums.payment.RefundInitiator;
import shopping.international.domain.model.enums.payment.RefundReasonCode;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.domain.model.vo.payment.RefundNo;
import shopping.international.domain.model.vo.payment.RefundTarget;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.domain.service.orders.IOrderService;
import shopping.international.domain.service.payment.impl.PaymentService;
import shopping.international.infrastructure.dao.orders.*;
import shopping.international.infrastructure.dao.orders.po.*;
import shopping.international.infrastructure.dao.payment.PaymentOrderMapper;
import shopping.international.infrastructure.dao.payment.PaymentRefundItemMapper;
import shopping.international.infrastructure.dao.payment.PaymentRefundMapper;
import shopping.international.infrastructure.dao.payment.po.PaymentOrderPO;
import shopping.international.infrastructure.dao.payment.po.PaymentRefundItemPO;
import shopping.international.infrastructure.dao.payment.po.PaymentRefundPO;
import shopping.international.infrastructure.dao.products.ProductSkuMapper;
import shopping.international.infrastructure.dao.products.po.ProductSkuPO;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.exceptions.PaymentOrderMissingException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
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
@Slf4j
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
    /**
     * payment_order Mapper (用于同库事务内创建占位支付单, 以及在订单侧保证跨域一致性)
     */
    private final PaymentOrderMapper paymentOrderMapper;
    /**
     * payment_refund Mapper (用于订单域确认退款时落库退款事实表)
     */
    private final PaymentRefundMapper paymentRefundMapper;
    /**
     * payment_refund_item Mapper
     */
    private final PaymentRefundItemMapper paymentRefundItemMapper;

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

        // 7) 创建占位支付单 (channel=NONE, status=NONE), 用于后续 /payments/paypal/checkout 将其升级为真实支付单
        PaymentOrderPO placeholderPayment = PaymentOrderPO.builder()
                .orderId(orderId)
                .externalId(null)
                .captureId(null)
                .channel(PayChannel.NONE.name())
                .amount(ordersPO.getPayAmount())
                .currency(ordersPO.getCurrency())
                .status(PayStatus.NONE.name())
                .requestPayload(null)
                .responsePayload(null)
                .notifyPayload(null)
                .lastPolledAt(null)
                .lastNotifiedAt(null)
                .build();
        paymentOrderMapper.insert(placeholderPayment);

        // 8) 记录当前有效支付单: 初始指向占位支付单 (后续 checkout 将其升级为真实通道支付单)
        int activeSet = ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, orderId)
                .isNull(OrdersPO::getActivePaymentId)
                .set(OrdersPO::getActivePaymentId, placeholderPayment.getId()));
        if (activeSet <= 0)
            throw new ConflictException("写入 active_payment_id 失败");

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

        // 同事务内同步支付单状态 (跨域一致性保证): 订单取消后, 关闭该订单下所有非 SUCCESS 的支付单
        syncPaymentOrdersAfterOrderCancelled(order.getId(), order.getOrderNo().getValue());

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
                .and(w -> w
                        .eq(OrdersPO::getStatus, OrderStatus.PAID).or()
                        .eq(OrdersPO::getStatus, OrderStatus.FULFILLED)
                )
                .set(OrdersPO::getStatus, order.getStatus().name())
                .set(OrdersPO::getRefundReasonSnapshot, toJsonOrNull(order.getLastRefundReason()))
        );
        insertStatusLog(order.getId(), eventSource, fromStatus, order.getStatus(), note);
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单退款申请后回读失败"));
    }

    /**
     * 确认退款第一步, 锁单校验 + 计算拆分 + 创建退款事实 (INIT)
     *
     * @param orderNo     订单号, 用于定位需要处理的订单
     * @param eventSource 事件来源, 指示触发此操作的具体原因或上下文
     * @param note        备注信息, 可为空, 提供额外的操作说明或记录
     * @param refundPlan  退款计划
     * @return 返回一个 {@link ConfirmRefundPrepared} 对象, 包含了准备退款所需的全部信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ConfirmRefundPrepared prepareConfirmRefund(@NotNull OrderNo orderNo,
                                                               @NotNull OrderStatusEventSource eventSource,
                                                               @Nullable String note,
                                                               @NotNull ConfirmRefundPlan refundPlan) {
        OrdersPO locked = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getOrderNo, orderNo.getValue())
                .last("limit 1 for update"));
        if (locked == null)
            throw new IllegalParamException("订单不存在");

        OrderStatus fromStatus = OrderStatus.valueOf(locked.getStatus());
        if (fromStatus != OrderStatus.REFUNDING && fromStatus != OrderStatus.REFUNDED)
            throw new ConflictException("订单状态为: " + locked.getStatus() + " 不允许确认退款");

        if (fromStatus == OrderStatus.REFUNDED) {
            // 幂等: 已退款直接返回
            PaymentRefundPO latest = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                    .eq(PaymentRefundPO::getOrderId, locked.getId())
                    .eq(PaymentRefundPO::getStatus, RefundStatus.SUCCESS)
                    .orderByDesc(PaymentRefundPO::getCreatedAt)
                    .last("limit 1"));
            if (latest == null)
                throw new ConflictException("订单已退款但退款单不存在, orderNo: " + locked.getOrderNo());

            PaymentOrderPO payment = paymentOrderMapper.selectById(latest.getPaymentOrderId());
            if (payment == null)
                throw PaymentOrderMissingException.of("订单关联支付单不存在, orderNo: " + locked.getOrderNo());

            String paypalOrderId = requireNotBlankOrThrow(payment.getExternalId(), "支付单 external_id 为空");
            return buildConfirmRefundPrepared(locked, latest, payment, paypalOrderId);
        }

        // 幂等: 若存在进行中的退款单, 直接返回 (避免重复调用网关)
        PaymentOrderPO paidPayment = findPaidPaymentOrThrow(locked.getId(), locked.getOrderNo());
        String paypalOrderId = requireNotBlankOrThrow(paidPayment.getExternalId(), "支付单 external_id 为空, 无法退款");
        PaymentRefundPO existing = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getPaymentOrderId, paidPayment.getId())
                .in(PaymentRefundPO::getStatus, RefundStatus.INIT.name(), RefundStatus.PENDING.name())
                .orderByDesc(PaymentRefundPO::getCreatedAt)
                .last("limit 1"));

        if (existing != null)
            return buildConfirmRefundPrepared(locked, existing, paidPayment, paypalOrderId);

        // 全局保护: 若支付单下已存在成功退款, 则不再发起新的网关退款 (避免跨来源重复退款)
        PaymentRefundPO success = paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getPaymentOrderId, paidPayment.getId())
                .eq(PaymentRefundPO::getStatus, RefundStatus.SUCCESS.name())
                .orderByDesc(PaymentRefundPO::getCreatedAt)
                .last("limit 1"));
        if (success != null)
            return buildConfirmRefundPrepared(locked, success, paidPayment, paypalOrderId);

        refundPlan.validate();

        String refundNo = RefundNo.generate().getValue();
        String clientRefundNo = PaymentService.RefundClientRefundNoUtils.admin(paidPayment.getId(), refundNo);
        Long itemsAmount = refundPlan.fullRefund() ? null : refundPlan.itemsAmountMinor();
        Long shippingAmount = refundPlan.fullRefund() ? null : refundPlan.shippingAmountMinor();

        PaymentRefundPO refundPO = PaymentRefundPO.builder()
                .refundNo(refundNo)
                .orderId(locked.getId())
                .paymentOrderId(paidPayment.getId())
                .externalRefundId(null)
                .clientRefundNo(clientRefundNo)
                .amount(refundPlan.refundAmountMinor())
                .currency(locked.getCurrency())
                .itemsAmount(itemsAmount)
                .shippingAmount(shippingAmount)
                .status(RefundStatus.INIT.name())
                .reasonCode(RefundReasonCode.CUSTOMER_REQUEST.name())
                .reasonText(note == null ? null : (note.length() <= 255 ? note : note.substring(0, 255)))
                .initiator(eventSource == OrderStatusEventSource.ADMIN ? RefundInitiator.ADMIN.name() : RefundInitiator.SYSTEM.name())
                .ticketId(null)
                .requestPayload(null)
                .responsePayload(null)
                .notifyPayload(null)
                .lastPolledAt(null)
                .lastNotifiedAt(null)
                .build();
        paymentRefundMapper.insert(refundPO);

        if (!refundPlan.items().isEmpty()) {
            List<PaymentRefundItemPO> items = refundPlan.items().stream()
                    .map(it -> PaymentRefundItemPO.builder()
                            .refundId(refundPO.getId())
                            .orderItemId(it.orderItemId())
                            .quantity(it.quantity())
                            .amount(it.amountMinor())
                            .reason(it.reason())
                            .build()
                    )
                    .toList();
            paymentRefundItemMapper.insert(items);
        }

        return new ConfirmRefundPrepared(
                locked.getId(),
                locked.getOrderNo(),
                paidPayment.getId(),
                paypalOrderId,
                paidPayment.getCaptureId(),
                refundNo,
                refundPO.getId(),
                clientRefundNo,
                refundPlan.refundAmountMinor(),
                locked.getCurrency(),
                itemsAmount,
                shippingAmount,
                refundPlan.fullRefund(),
                true
        );
    }

    /**
     * 确认退款第二步, 锁单回填网关退款结果 + 必要时推进订单/回补库存
     *
     * @param cmd 确认退款绑定命令, 包含了需要绑定的退款信息
     * @return 更新后的订单, 包含了最新的退款状态和相关信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull Order bindConfirmRefundResult(@NotNull ConfirmRefundBindCommand cmd) {
        cmd.validate();

        OrdersPO locked = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, cmd.orderId())
                .last("limit 1 for update"));
        if (locked == null)
            throw new ConflictException("订单不存在");
        if (!cmd.orderNo().equals(locked.getOrderNo()))
            throw new ConflictException("orderNo 不匹配");

        int update = paymentRefundMapper.update(null, new LambdaUpdateWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getId, cmd.refundId())
                .eq(PaymentRefundPO::getOrderId, cmd.orderId())
                .eq(PaymentRefundPO::getPaymentOrderId, cmd.paymentOrderId())
                .in(PaymentRefundPO::getStatus, RefundStatus.INIT.name(), RefundStatus.PENDING.name())
                .set(PaymentRefundPO::getExternalRefundId, cmd.externalRefundId())
                .set(PaymentRefundPO::getStatus, cmd.status().name())
                .set(PaymentRefundPO::getRequestPayload, cmd.requestPayload())
                .set(PaymentRefundPO::getResponsePayload, cmd.responsePayload()));
        if (update <= 0) {
            PaymentRefundPO refundPO = paymentRefundMapper.selectById(cmd.refundId());
            if (refundPO == null)
                throw new ConflictException("退款单不存在, 退款单 ID: " + cmd.refundId() + ", 订单 ID: " + cmd.orderId() + ", 支付单 ID: " + cmd.paymentOrderId());

            // 并发幂等: 退款单可能已被 WebHook/兜底任务推进为 SUCCESS, 但订单仍处于 REFUNDING, 这里补推进一次
            if (RefundStatus.SUCCESS.name().equals(refundPO.getStatus()) && OrderStatus.REFUNDING.name().equals(locked.getStatus())) {
                Order order = assembleOrder(locked);
                boolean fullRefund = refundPO.getItemsAmount() == null && refundPO.getShippingAmount() == null;
                applyRefundSuccessAndRestock(
                        order,
                        OrderStatusEventSource.ADMIN,
                        cmd.note(),
                        cmd.paymentOrderId(),
                        cmd.refundId(),
                        fullRefund
                );
            }

            return findOrderDetail(OrderNo.of(cmd.orderNo()))
                    .orElseThrow(() -> new ConflictException("订单退款回填后回读失败"));
        }

        if (cmd.status() == RefundStatus.SUCCESS) {
            Order order = assembleOrder(locked);
            applyRefundSuccessAndRestock(
                    order,
                    OrderStatusEventSource.ADMIN,
                    cmd.note(),
                    cmd.paymentOrderId(),
                    cmd.refundId(),
                    cmd.fullRefund()
            );
        }
        return findOrderDetail(OrderNo.of(cmd.orderNo()))
                .orElseThrow(() -> new ConflictException("订单退款回填后回读失败"));
    }

    /**
     * 处理退款成功并回补库存的操作
     * 此方法会执行以下步骤:
     * <ol>
     *     <li>同步支付单状态: 将原支付单的状态从成功改为关闭</li>
     *     <li>同步订单的冗余支付状态, 以确保与支付单状态一致</li>
     *     <li>推进订单状态为已退款, 并记录状态变更日志</li>
     *     <li>根据是否全额退款以及提供的退款明细, 回补相应数量的商品库存</li>
     * </ul>
     *
     * @param order         订单对象, 包含需要处理退款和回补库存的订单信息
     * @param eventSource   事件来源, 用于记录状态变更日志
     * @param note          可选参数, 退款操作的备注信息
     * @param paidPaymentId 原支付单的ID, 用于更新支付单状态
     * @param refundId      退款单ID, 如果不是全额退款, 则根据此ID获取退款项来确定回补库存的数量
     * @param fullRefund    是否为全额退款, 若为true则按照整单回补库存, 否则依据退款明细回补
     * @throws PaymentOrderMissingException 当指定的支付单不存在时抛出
     * @throws ConflictException            当订单状态不允许确认退款或订单不存在时抛出
     */
    private void applyRefundSuccessAndRestock(@NotNull Order order,
                                              @NotNull OrderStatusEventSource eventSource,
                                              @Nullable String note,
                                              @NotNull Long paidPaymentId,
                                              @NotNull Long refundId,
                                              boolean fullRefund) {
        // 1) 同事务内同步支付单状态: 退款成功后, 关闭原支付单 (SUCCESS/EXCEPTION -> CLOSED)
        int closed = paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paidPaymentId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.SUCCESS.name(), PaymentStatus.EXCEPTION.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
        if (closed <= 0) {
            PaymentOrderPO reRead = paymentOrderMapper.selectById(paidPaymentId);
            if (reRead == null)
                throw PaymentOrderMissingException.of("订单关联支付单不存在, orderNo: " + order.getOrderNo().getValue());
        }

        // 2) 同事务内同步 orders 冗余支付状态 (SUCCESS/EXCEPTION -> CLOSED), 避免与 payment_order 冗余不一致
        ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, order.getId())
                .in(OrdersPO::getPayStatus, PayStatus.SUCCESS.name(), PayStatus.EXCEPTION.name())
                .set(OrdersPO::getPayStatus, PayStatus.CLOSED.name()));

        // 3) 推进订单状态为 REFUNDED (CAS)
        int updated = ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                .eq(OrdersPO::getId, order.getId())
                .eq(OrdersPO::getStatus, OrderStatus.REFUNDING.name())
                .set(OrdersPO::getStatus, OrderStatus.REFUNDED.name()));
        if (updated > 0) {
            insertStatusLog(order.getId(), eventSource, OrderStatus.REFUNDING, OrderStatus.REFUNDED, note);
        } else {
            OrdersPO reread = ordersMapper.selectById(order.getId());
            if (reread == null)
                throw new ConflictException("订单不存在");
            // 幂等: 已 REFUNDED 直接返回 (避免重复回补库存造成经济损失)
            if (OrderStatus.REFUNDED.name().equals(reread.getStatus()))
                return;
            throw new ConflictException("订单状态为: " + reread.getStatus() + " 不允许确认退款");
        }

        // 4) 回补库存: 若提供 refund_item 则按明细回补, 否则按整单回补
        if (fullRefund) {
            reserveStockAndWriteInventoryLogs(order.getId(), order.getItems(), InventoryChangeType.RESTOCK, note);
            return;
        }
        List<PaymentRefundItemPO> items = paymentRefundItemMapper.selectList(
                new LambdaQueryWrapper<PaymentRefundItemPO>()
                        .eq(PaymentRefundItemPO::getRefundId, refundId)
        );
        if (items == null || items.isEmpty())
            return;

        List<OrderItem> orderItems = order.getItems();
        Map<Long, OrderItem> orderItemMap = orderItems.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(OrderItem::getId, i -> i, (a, b) -> a));
        Map<Long, Integer> skuQtyMap = new HashMap<>();
        for (PaymentRefundItemPO it : items) {
            if (it == null || it.getOrderItemId() == null || it.getQuantity() == null)
                continue;
            OrderItem oi = orderItemMap.get(it.getOrderItemId());
            if (oi == null)
                continue;
            skuQtyMap.merge(oi.getSkuId(), it.getQuantity(), Integer::sum);
        }
        List<OrderItem> restockItems = skuQtyMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && e.getValue() > 0)
                .map(e ->
                        new OrderItem(
                                null,
                                order.getId(),
                                null,
                                e.getKey(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                e.getValue(),
                                null,
                                null
                        )
                )
                .toList();

        if (restockItems.isEmpty())
            return;

        reserveStockAndWriteInventoryLogs(order.getId(), restockItems, InventoryChangeType.RESTOCK, note);
    }

    /**
     * 根据条件尝试将订单状态从退款中{@link OrderStatus#REFUNDING}更新为已退款{@link OrderStatus#REFUNDED}
     * 仅当订单当前处于 REFUNDING 状态时, 才会进行状态变更; 否则方法直接返回不做任何处理
     *
     * @param target             退款目标信息 {@link RefundTarget}
     * @param refundResult       查询 Refund 结果
     * @param notifyPayload      回调请求体
     * @param refundResultStatus 查询 Refund 结果的状态
     * @param eventSource        订单状态变更事件来源 {@link OrderStatusEventSource}
     * @param note               备注信息, 记录退款成功的额外说明
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefundResult(@NotNull RefundTarget target, @NotNull IPayPalPort.GetRefundResult refundResult,
                                  @NotNull Map<String, Object> notifyPayload, @NotNull RefundStatus refundResultStatus,
                                  @NotNull OrderStatusEventSource eventSource, @NotNull String note) {
        OrdersPO orderPo = ordersMapper.selectOne(new LambdaQueryWrapper<OrdersPO>()
                .eq(OrdersPO::getId, target.orderId())
                .last("limit 1 for update"));
        if (orderPo == null)
            return;

        String notifyPayloadJson = toJsonOrNull(notifyPayload);
        // 1) 先尽量回填外部单号 (幂等: 仅在空值时回填)
        if (!target.externalRefundId().isBlank()) {
            paymentRefundMapper.update(null, new LambdaUpdateWrapper<PaymentRefundPO>()
                    .eq(PaymentRefundPO::getId, target.refundId())
                    .eq(PaymentRefundPO::getOrderId, target.orderId())
                    .eq(PaymentRefundPO::getPaymentOrderId, target.paymentOrderId())
                    .and(w -> w
                            .isNull(PaymentRefundPO::getExternalRefundId).or()
                            .eq(PaymentRefundPO::getExternalRefundId, "")
                    )
                    .set(PaymentRefundPO::getExternalRefundId, target.externalRefundId()));
        }

        // 2) 再尽量回填 payload/时间戳 (即便已是终态也允许补充审计信息)
        boolean shouldUpdatePayload = false;
        LambdaUpdateWrapper<PaymentRefundPO> payloadWrapper = new LambdaUpdateWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getId, target.refundId())
                .eq(PaymentRefundPO::getOrderId, target.orderId())
                .eq(PaymentRefundPO::getPaymentOrderId, target.paymentOrderId());
        if (!refundResult.responseJson().isBlank() && !"{}".equals(refundResult.responseJson())) {
            payloadWrapper.set(PaymentRefundPO::getResponsePayload, refundResult.responseJson());
            shouldUpdatePayload = true;
        }
        if (notifyPayloadJson != null && !notifyPayloadJson.isBlank() && !"{}".equals(notifyPayloadJson)) {
            payloadWrapper.set(PaymentRefundPO::getNotifyPayload, notifyPayloadJson);
            shouldUpdatePayload = true;
        }
        if (eventSource == OrderStatusEventSource.SCHEDULER) {
            payloadWrapper.set(PaymentRefundPO::getLastPolledAt, LocalDateTime.now());
            shouldUpdatePayload = true;
        }
        if (eventSource == OrderStatusEventSource.PAYMENT_CALLBACK) {
            payloadWrapper.set(PaymentRefundPO::getLastNotifiedAt, LocalDateTime.now());
            shouldUpdatePayload = true;
        }
        if (shouldUpdatePayload)
            paymentRefundMapper.update(null, payloadWrapper);

        // 3) 再尝试推进退款单状态 (仅允许 INIT/PENDING -> 终态), 保持幂等
        paymentRefundMapper.update(null, new LambdaUpdateWrapper<PaymentRefundPO>()
                .eq(PaymentRefundPO::getId, target.refundId())
                .eq(PaymentRefundPO::getOrderId, target.orderId())
                .eq(PaymentRefundPO::getPaymentOrderId, target.paymentOrderId())
                .in(PaymentRefundPO::getStatus, RefundStatus.INIT.name(), RefundStatus.PENDING.name())
                .set(PaymentRefundPO::getStatus, refundResultStatus.name()));

        if (refundResultStatus != RefundStatus.SUCCESS)
            return;
        // 4) 仅当订单处于 REFUNDING 时才推进为 REFUNDED 并回补库存, 其它状态仅关闭支付尝试, 避免误伤订单/库存
        if (OrderStatus.REFUNDING.name().equals(orderPo.getStatus())) {
            PaymentRefundPO refundPO = paymentRefundMapper.selectById(target.refundId());
            if (refundPO == null)
                return;
            if (RefundStatus.SUCCESS != RefundStatus.valueOf(refundPO.getStatus()))
                return;
            boolean fullRefund = refundPO.getItemsAmount() == null && refundPO.getShippingAmount() == null;
            Order order = assembleOrder(orderPo);
            applyRefundSuccessAndRestock(
                    order,
                    eventSource,
                    note,
                    target.paymentOrderId(),
                    target.refundId(),
                    fullRefund
            );
            return;
        }

        // 自动退款/对账退款等非 REFUNDING 场景: 只关闭支付尝试与必要的 orders 冗余支付状态
        PaymentRefundPO refundPO = paymentRefundMapper.selectById(target.refundId());
        if (refundPO == null
                || !target.orderId().equals(refundPO.getOrderId())
                || !target.paymentOrderId().equals(refundPO.getPaymentOrderId())
                || RefundStatus.SUCCESS != RefundStatus.valueOf(refundPO.getStatus()))
            return;
        closePaymentAttemptAfterRefundSuccess(orderPo, target.paymentOrderId());
    }

    /**
     * 退款成功后关闭支付尝试, 并在命中订单当前有效支付尝试时同步 orders.pay_status -> CLOSED
     *
     * <p>用于自动退款/对账退款等 "订单不在 REFUNDING" 的场景, 避免因为订单状态不匹配而回滚导致丢账</p>
     *
     * @param orderPo        订单 PO
     * @param paymentOrderId 支付单 ID
     */
    private void closePaymentAttemptAfterRefundSuccess(@NotNull OrdersPO orderPo, @NotNull Long paymentOrderId) {
        PaymentOrderPO payment = paymentOrderMapper.selectById(paymentOrderId);
        if (payment == null)
            return;
        if (!Objects.equals(orderPo.getId(), payment.getOrderId()))
            return;

        // 兼容旧数据: 若 active_payment_id 为空但 orders.payment_external_id 已指向该 external_id, 则补齐 active_payment_id
        if (orderPo.getActivePaymentId() == null
                && payment.getExternalId() != null
                && !payment.getExternalId().isBlank()
                && payment.getExternalId().equals(orderPo.getPaymentExternalId())) {
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, orderPo.getId())
                    .isNull(OrdersPO::getActivePaymentId)
                    .set(OrdersPO::getActivePaymentId, paymentOrderId));
            orderPo.setActivePaymentId(paymentOrderId);
        }

        boolean isActiveAttempt = orderPo.getActivePaymentId() != null && orderPo.getActivePaymentId().equals(paymentOrderId);

        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getId, paymentOrderId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.SUCCESS.name(), PaymentStatus.EXCEPTION.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));

        if (isActiveAttempt) {
            ordersMapper.update(null, new LambdaUpdateWrapper<OrdersPO>()
                    .eq(OrdersPO::getId, orderPo.getId())
                    .in(OrdersPO::getPayStatus, PayStatus.SUCCESS.name(), PayStatus.EXCEPTION.name())
                    .set(OrdersPO::getPayStatus, PayStatus.CLOSED.name()));
        }
    }

    /**
     * 订单取消后同步 payment_order 状态 (跨域一致性)
     *
     * <p>规则: 关闭该订单下所有非 SUCCESS 的支付单, 将其推进为 CLOSED。</p>
     *
     * @param orderId 订单 ID
     * @param orderNo 订单号 (用于错误信息)
     */
    private void syncPaymentOrdersAfterOrderCancelled(@NotNull Long orderId, @NotNull String orderNo) {
        List<PaymentOrderPO> payments = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId));
        if (payments == null || payments.isEmpty())
            throw PaymentOrderMissingException.of("订单关联支付单不存在, orderNo: " + orderNo);

        // 关闭占位/NONE 或待支付支付单 (避免用户取消订单后仍可继续支付)
        paymentOrderMapper.update(null, new LambdaUpdateWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId)
                .in(PaymentOrderPO::getStatus, PaymentStatus.NONE.name(), PaymentStatus.INIT.name(), PaymentStatus.PENDING.name())
                .set(PaymentOrderPO::getStatus, PaymentStatus.CLOSED.name()));
    }

    /**
     * 查找订单下已支付成功的支付单 (用于退款)
     *
     * @param orderId 订单 ID
     * @param orderNo 订单号 (用于错误信息)
     * @return 支付单
     */
    private @NotNull PaymentOrderPO findPaidPaymentOrThrow(@NotNull Long orderId, @NotNull String orderNo) {
        List<PaymentOrderPO> all = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId));
        if (all == null || all.isEmpty())
            throw PaymentOrderMissingException.of("订单关联支付单不存在, orderNo: " + orderNo);

        PaymentOrderPO paid = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderPO>()
                .eq(PaymentOrderPO::getOrderId, orderId)
                .eq(PaymentOrderPO::getStatus, PaymentStatus.SUCCESS.name())
                .orderByDesc(PaymentOrderPO::getUpdatedAt)
                .last("limit 1"));
        if (paid == null)
            throw new ConflictException("未找到可退款的成功支付单, orderNo: " + orderNo);
        return paid;
    }

    /**
     * 检查给定字符串是否非空且不只包含空白字符, 如果条件不满足则抛出异常
     *
     * @param v   要检查的字符串, 可以为 null
     * @param msg 当 v 为空或仅包含空白时抛出异常所使用的消息
     * @return 去掉首尾空白后的字符串
     * @throws IllegalArgumentException 如果 v 为空或仅由空白字符组成
     */
    private static @NotNull String requireNotBlankOrThrow(@Nullable String v, @NotNull String msg) {
        requireNotBlank(v, msg);
        return v.strip();
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
                .and(w -> w
                        .eq(OrdersPO::getStatus, OrderStatus.CANCELLED).or()
                        .eq(OrdersPO::getStatus, OrderStatus.REFUNDED).or()
                        .eq(OrdersPO::getStatus, OrderStatus.FULFILLED)
                )
                .set(OrdersPO::getStatus, order.getStatus().name())
        );
        insertStatusLog(order.getId(), eventSource, fromStatus, order.getStatus(), note);
        return findOrderDetail(order.getOrderNo()).orElseThrow(() -> new ConflictException("订单关闭后回读失败"));
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
    @Override
    public void insertStatusLog(Long orderId, OrderStatusEventSource source, @Nullable OrderStatus fromStatus,
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


    // ========================= 内部装配与落库辅助 =========================

    /**
     * 构建 ConfirmRefundPrepared 对象, 该对象用于确认退款准备
     *
     * @param orderPO           订单持久化对象
     * @param refundPO          退款持久化对象
     * @param paymentPO         支付持久化对象
     * @param externalPaymentId 外部支付系统的支付 ID
     * @return 一个已构建的 ConfirmRefundPrepared 实例, 包含了完成退款所需的所有必要信息
     */
    @NotNull
    private ConfirmRefundPrepared buildConfirmRefundPrepared(OrdersPO orderPO, PaymentRefundPO refundPO, PaymentOrderPO paymentPO, String externalPaymentId) {
        String clientRefundNo = refundPO.getClientRefundNo() == null || refundPO.getClientRefundNo().isBlank()
                ? PaymentService.RefundClientRefundNoUtils.admin(refundPO.getPaymentOrderId(), refundPO.getRefundNo())
                : refundPO.getClientRefundNo();
        return new ConfirmRefundPrepared(
                orderPO.getId(),
                orderPO.getOrderNo(),
                paymentPO.getId(),
                externalPaymentId,
                paymentPO.getCaptureId(),
                refundPO.getRefundNo(),
                refundPO.getId(),
                clientRefundNo,
                refundPO.getAmount(),
                orderPO.getCurrency(),
                refundPO.getItemsAmount(),
                refundPO.getShippingAmount(),
                refundPO.getItemsAmount() == null && refundPO.getShippingAmount() == null,
                false
        );
    }

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
        List<OrderItem> items = (itemPos == null || itemPos.isEmpty()) ? List.of()
                : itemPos.stream()
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
                    asString(map.get("phoneCountryCode")),
                    asString(map.get("phoneNationalNumber")),
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
