package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.orders.IOrderRepository;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.*;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.types.exceptions.IllegalParamException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        OrderStatus from = order.getStatus();
        return orderRepository.confirmRefundAndRestock(order, from, OrderStatusEventSource.ADMIN, note, cmd);
    }

    /**
     * 兜底任务: 扫描并同步非终态退款单
     *
     * <p>用于兜底处理以下场景:</p>
     * <ul>
     *     <li>网关退款返回 PENDING, 回调未到或未成功处理</li>
     *     <li>运维补偿/重试时需要刷新退款状态并推进订单/库存</li>
     * </ul>
     *
     * @param limit 单批最大数量
     * @return 本次处理的退款单数量
     */
    @Override
    public int syncNonFinalRefunds(int limit) {
        return orderRepository.syncNonFinalRefunds(limit);
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
}
