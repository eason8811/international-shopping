package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.payment.IPayPalPort;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderItem;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.enums.payment.paypal.PayPalRefundStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.types.exceptions.IllegalParamException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理侧订单领域服务默认实现
 */
@Service
@RequiredArgsConstructor
public class AdminOrderService implements IAdminOrderService {

    /**
     * 订单仓储
     */
    private final IOrderRepository orderRepository;
    /**
     * PayPal 网关端口 (用于两段式确认退款)
     */
    private final IPayPalPort payPalPort;

    /**
     * 查询订单列表 (管理侧)
     *
     * @param criteria  查询条件
     * @param pageQuery 分页条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<AdminOrderListItemView> list(@NotNull AdminOrderSearchCriteria criteria, @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<AdminOrderListItemView> items = orderRepository.pageAdminOrders(criteria, offset, limit);
        long total = orderRepository.countAdminOrders(criteria);
        return new PageResult<>(items, total);
    }

    /**
     * 获取订单详情 (管理侧)
     *
     * @param orderNo 订单号
     * @return Optional
     */
    @Override
    public @NotNull Optional<Order> getDetail(@NotNull OrderNo orderNo) {
        return orderRepository.findOrderDetail(orderNo);
    }

    /**
     * 列出待支付且已超时的订单 (用于兜底)
     *
     * @param createdBefore 创建时间上限
     * @param limit         最大返回数量
     * @return 订单列表
     */
    @Override
    public @NotNull List<Order> listTimeoutCandidates(@NotNull LocalDateTime createdBefore, int limit) {
        return orderRepository.listTimeoutCandidates(createdBefore, limit);
    }

    /**
     * 系统/调度侧取消未支付订单 (用于超时兜底)
     *
     * @param orderNo 订单号
     * @param reason  取消原因
     * @param source  事件来源
     * @return 取消后的订单
     */
    @Override
    public @NotNull Order cancelUnpaid(@NotNull OrderNo orderNo, @NotNull String reason, @NotNull OrderStatusEventSource source) {
        Order order = orderRepository.findOrderDetail(orderNo)
                .orElseThrow(() -> new IllegalParamException("订单不存在"));
        OrderStatus from = order.getStatus();
        order.cancel(CancelReason.of(reason), source);
        return orderRepository.cancelAndReleaseStock(order, from, source, reason);
    }

    /**
     * 关闭订单 (管理侧)
     *
     * @param orderNo 订单号
     * @param reason  关闭原因
     * @return 关闭后的订单
     */
    @Override
    public @NotNull Order close(@NotNull OrderNo orderNo, @NotNull String reason) {
        Order order = orderRepository.findOrderDetail(orderNo)
                .orElseThrow(() -> new IllegalParamException("订单不存在"));
        OrderStatus from = order.getStatus();
        order.close(reason);
        return orderRepository.close(order, from, OrderStatusEventSource.ADMIN, reason);
    }

    /**
     * 确认退款 (管理侧)
     *
     * @param orderNo 订单号
     * @param note    备注 (可为空)
     * @param cmd     退款明细/金额拆分 (可为空, 为空代表整单退款)
     * @return 更新后的订单聚合
     */
    @Override
    public @NotNull Order confirmRefund(@NotNull OrderNo orderNo, @Nullable String note, @Nullable ConfirmRefundCommand cmd) {
        Order order = orderRepository.findOrderDetail(orderNo)
                .orElseThrow(() -> new IllegalParamException("订单不存在"));
        ConfirmRefundPlan refundPlan = calculate(order, cmd);

        ConfirmRefundPrepared prepared = orderRepository.prepareConfirmRefund(orderNo, OrderStatusEventSource.ADMIN, note, refundPlan);
        if (!prepared.shouldCallGateway())
            return orderRepository.findOrderDetail(orderNo)
                    .orElseThrow(() -> new IllegalParamException("订单不存在"));

        String captureId = prepared.captureId();
        if (captureId == null || captureId.isBlank()) {
            IPayPalPort.GetOrderResult paypalOrder = payPalPort.getOrder(prepared.paypalOrderId());
            captureId = paypalOrder.captureId();
        }
        if (captureId == null || captureId.isBlank())
            throw new IllegalParamException("无法获取 PayPal capture_id, 无法发起退款");

        IPayPalPort.RefundCaptureResult refunded = payPalPort.refundCapture(
                new IPayPalPort.RefundCaptureCommand(
                        prepared.clientRefundNo(),
                        captureId.strip(),
                        prepared.refundAmountMinor(),
                        prepared.currency(),
                        note
                )
        );

        PayPalRefundStatus paypalStatus = PayPalRefundStatus.from(refunded.status());
        RefundStatus status = switch (paypalStatus) {
            case COMPLETED -> RefundStatus.SUCCESS;
            case PENDING, UNKNOWN -> RefundStatus.PENDING;
            case FAILED, CANCELLED -> RefundStatus.FAIL;
        };

        return orderRepository.bindConfirmRefundResult(
                new ConfirmRefundBindCommand(
                        prepared.orderId(),
                        prepared.orderNo(),
                        prepared.paymentOrderId(),
                        prepared.refundId(),
                        refunded.refundId(),
                        status,
                        refunded.requestJson(),
                        refunded.responseJson(),
                        note,
                        prepared.fullRefund()
                )
        );
    }

    /**
     * 查询订单状态流转日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @Override
    public @NotNull List<OrderStatusLog> listStatusLogs(@NotNull OrderNo orderNo) {
        return orderRepository.listStatusLogs(orderNo);
    }

    /**
     * 查询订单关联库存日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @Override
    public @NotNull List<InventoryLog> listInventoryLogs(@NotNull OrderNo orderNo) {
        return orderRepository.listInventoryLogsByOrderNo(orderNo);
    }

    /**
     * 分页查询库存日志
     *
     * @param criteria  查询条件
     * @param pageQuery 分页条件
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<InventoryLog> pageInventoryLogs(@NotNull InventoryLogSearchCriteria criteria, @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<InventoryLog> items = orderRepository.pageInventoryLogs(criteria, offset, limit);
        long total = orderRepository.countInventoryLogs(criteria);
        return new PageResult<>(items, total);
    }

    // ================================ 内部工具 ================================

    /**
     * 计算退款计划的详细信息
     *
     * @param order 订单对象, 包含订单的所有信息 如订单项 {@code OrderItem} 和支付金额等
     * @param cmd   退款命令, 可以为空 如果为空 则表示全额退款 否则根据命令中的具体信息计算部分退款
     * @return 一个 {@link ConfirmRefundPlan} 对象, 包括是否为全额退款 退款总金额 项目退款金额 运费退款金额以及具体的退款条目列表
     * @throws IllegalParamException 当请求的退款数量超出订单明细数量 或者退款金额小于等于 0 或者超过已支付金额时抛出此异常
     */
    private static @NotNull ConfirmRefundPlan calculate(@NotNull Order order, @Nullable ConfirmRefundCommand cmd) {
        boolean fullRefund = ConfirmRefundCommand.isEmpty(cmd);
        if (fullRefund) {
            return new ConfirmRefundPlan(
                    true,
                    order.getPayAmount().getAmountMinor(),
                    null,
                    null,
                    List.of()
            );
        }

        List<OrderItem> orderItems = order.getItems();
        List<ConfirmRefundCommand.RefundItem> requestedItems = cmd.items();

        Map<Long, OrderItem> orderItemMap = orderItems.stream()
                .filter(Objects::nonNull)
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(OrderItem::getId, i -> i, (a, b) -> a));

        List<ConfirmRefundPlanItem> plannedItems = new ArrayList<>();
        long itemsAmountMinor = 0L;
        if (!requestedItems.isEmpty()) {
            for (ConfirmRefundCommand.RefundItem r : requestedItems) {
                OrderItem oi = orderItemMap.get(r.orderItemId());
                if (oi == null)
                    throw new IllegalParamException("退款明细不属于该订单: orderItemId: " + r.orderItemId());
                if (r.quantity() > oi.getQuantity())
                    throw new IllegalParamException("退款数量超出订单明细数量: orderItemId: " + r.orderItemId());

                long amountMinor;
                if (r.amountMinor() != null) {
                    amountMinor = r.amountMinor();
                } else {
                    long subtotal = oi.getSubtotalAmount().getAmountMinor();
                    int totalQty = oi.getQuantity();
                    amountMinor = BigDecimal.valueOf(subtotal)
                            .multiply(BigDecimal.valueOf(r.quantity()))
                            .divide(BigDecimal.valueOf(totalQty), 0, RoundingMode.HALF_UP)
                            .longValueExact();
                }
                if (amountMinor <= 0)
                    throw new IllegalParamException("退款明细金额必须大于 0: orderItemId: " + r.orderItemId());

                itemsAmountMinor = Math.addExact(itemsAmountMinor, amountMinor);
                String reason = r.reason();
                if (reason != null && reason.length() > 255)
                    reason = reason.substring(0, 255);
                plannedItems.add(new ConfirmRefundPlanItem(r.orderItemId(), r.quantity(), amountMinor, reason));
            }
        } else if (cmd.itemsAmountMinor() != null) {
            itemsAmountMinor = cmd.itemsAmountMinor();
        }

        long shippingAmountMinor = 0L;
        if (cmd.shippingAmountMinor() != null)
            shippingAmountMinor = cmd.shippingAmountMinor();

        long refundAmountMinor = Math.addExact(itemsAmountMinor, shippingAmountMinor);
        if (refundAmountMinor <= 0)
            throw new IllegalParamException("退款金额必须大于 0");
        long paid = order.getPayAmount().getAmountMinor();
        if (refundAmountMinor > paid)
            throw new IllegalParamException("退款金额不能大于订单支付金额");

        return new ConfirmRefundPlan(
                false,
                refundAmountMinor,
                itemsAmountMinor,
                shippingAmountMinor,
                plannedItems
        );
    }
}
