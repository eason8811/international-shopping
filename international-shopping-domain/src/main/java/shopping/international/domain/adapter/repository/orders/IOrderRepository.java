package shopping.international.domain.adapter.repository.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.vo.orders.AddressSnapshot;
import shopping.international.domain.model.vo.orders.AdminOrderSearchCriteria;
import shopping.international.domain.model.vo.orders.InventoryLogSearchCriteria;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.domain.service.orders.IOrderService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单聚合仓储接口 (面向订单域)
 *
 * <p>职责:</p>
 * <ul>
 *     <li>对 {@code orders / order_item} 进行组合装配与持久化</li>
 *     <li>对状态流转日志 {@code order_status_log} 与库存日志 {@code inventory_log} 提供写入与查询能力</li>
 *     <li>对管理侧订单列表与审计查询提供高性能的只读查询</li>
 * </ul>
 */
public interface IOrderRepository {
    // ========================= 用户侧查询 =========================

    /**
     * 查询用户侧订单摘要列表
     *
     * @param userId      用户 ID
     * @param status      状态过滤, 可为空
     * @param createdFrom 创建时间起 (含), 可为空
     * @param createdTo   创建时间止 (含), 可为空
     * @param offset      偏移量, 从 0 开始
     * @param limit       单页数量
     * @return 摘要列表
     */
    @NotNull
    List<IOrderService.OrderSummaryRow> pageUserOrderSummaries(@NotNull Long userId,
                                                               @Nullable OrderStatus status,
                                                               @Nullable LocalDateTime createdFrom,
                                                               @Nullable LocalDateTime createdTo,
                                                               int offset, int limit);

    /**
     * 统计用户侧订单摘要数量
     *
     * @param userId      用户 ID
     * @param status      状态过滤, 可为空
     * @param createdFrom 创建时间起 (含), 可为空
     * @param createdTo   创建时间止 (含), 可为空
     * @return 总数
     */
    long countUserOrderSummaries(@NotNull Long userId,
                                 @Nullable OrderStatus status,
                                 @Nullable LocalDateTime createdFrom,
                                 @Nullable LocalDateTime createdTo);

    /**
     * 查询用户侧订单详情 (含明细)
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<Order> findUserOrderDetail(@NotNull Long userId, @NotNull OrderNo orderNo);

    // ========================= 管理侧查询 =========================

    /**
     * 分页查询管理侧订单列表
     *
     * @param criteria 查询条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @return 订单列表行
     */
    @NotNull
    List<IAdminOrderService.AdminOrderListItemView> pageAdminOrders(@NotNull AdminOrderSearchCriteria criteria, int offset, int limit);

    /**
     * 统计管理侧订单数量
     *
     * @param criteria 查询条件
     * @return 总数
     */
    long countAdminOrders(@NotNull AdminOrderSearchCriteria criteria);

    /**
     * 查询管理侧订单详情 (含明细)
     *
     * @param orderNo 订单号
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<Order> findOrderDetail(@NotNull OrderNo orderNo);

    // ========================= 审计查询 =========================

    /**
     * 查询订单状态流转日志
     *
     * @param orderNo 订单号
     * @return 状态流转日志列表
     */
    @NotNull
    List<OrderStatusLog> listStatusLogs(@NotNull OrderNo orderNo);

    /**
     * 查询订单关联的库存变更日志
     *
     * @param orderNo 订单号
     * @return 库存变更日志列表
     */
    @NotNull
    List<InventoryLog> listInventoryLogsByOrderNo(@NotNull OrderNo orderNo);

    /**
     * 分页查询库存变更日志
     *
     * @param criteria 查询条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @return 库存变更日志列表
     */
    @NotNull
    List<InventoryLog> pageInventoryLogs(@NotNull InventoryLogSearchCriteria criteria, int offset, int limit);

    /**
     * 统计库存变更日志数量
     *
     * @param criteria 查询条件
     * @return 总数
     */
    long countInventoryLogs(@NotNull InventoryLogSearchCriteria criteria);

    // ========================= 写入 (事务下沉到基础设施实现) =========================

    /**
     * 创建订单并预占库存 (RESERVE)
     *
     * <p>典型落库动作:</p>
     * <ul>
     *     <li>插入 {@code orders}</li>
     *     <li>插入 {@code order_item}</li>
     *     <li>扣减 {@code product_sku.stock}</li>
     *     <li>插入 {@code inventory_log(change_type=RESERVE)}</li>
     *     <li>插入 {@code order_status_log(from=null,to=CREATED)}</li>
     *     <li>可选: 清理购物车勾选条目</li>
     *     <li>可选: 写入 {@code order_discount_applied}</li>
     * </ul>
     *
     * @param order                 订单聚合 (id 为空)
     * @param eventSource           状态日志来源
     * @param note                  状态日志备注 (可为空)
     * @param cartItemIdsToDelete   需要删除的购物车条目 ID 列表 (可为空)
     * @param discountAppliedCreates 折扣流水写入参数 (可为空)
     * @param idempotencyKey        幂等键 (可为空)
     * @return 保存后的订单聚合 (携带持久化 ID 与明细 ID)
     */
    @NotNull
    Order createOrderAndReserveStock(@NotNull Order order,
                                     @NotNull OrderStatusEventSource eventSource,
                                     @Nullable String note,
                                     @Nullable List<Long> cartItemIdsToDelete,
                                     @Nullable List<IOrderService.OrderDiscountApplied> discountAppliedCreates,
                                     @Nullable String idempotencyKey);

    /**
     * 更新订单收货地址快照
     *
     * @param order         订单聚合
     * @param addressSnapshot 新的地址快照
     * @return 更新后的订单聚合快照
     */
    @NotNull
    Order updateAddressSnapshot(@NotNull Order order, @NotNull AddressSnapshot addressSnapshot);

    /**
     * 取消订单并回补库存 (RELEASE)
     *
     * @param order       订单聚合 (已在聚合内完成状态变更)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        状态日志备注 (可为空)
     * @return 更新后的订单聚合
     */
    @NotNull
    Order cancelAndReleaseStock(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                @NotNull OrderStatusEventSource eventSource, @Nullable String note);

    /**
     * 发起退款申请 (REFUNDING)
     *
     * @param order       订单聚合 (已在聚合内完成状态变更)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        状态日志备注 (可为空)
     * @return 更新后的订单聚合
     */
    @NotNull
    Order requestRefund(@NotNull Order order, @NotNull OrderStatus fromStatus,
                        @NotNull OrderStatusEventSource eventSource, @Nullable String note);

    /**
     * 确认退款并回补库存 (RESTOCK)
     *
     * @param order       订单聚合 (已在聚合内完成状态变更)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        状态日志备注 (可为空)
     * @return 更新后的订单聚合
     */
    @NotNull
    Order confirmRefundAndRestock(@NotNull Order order, @NotNull OrderStatus fromStatus,
                                  @NotNull OrderStatusEventSource eventSource, @Nullable String note);

    /**
     * 关闭订单 (CLOSED)
     *
     * @param order       订单聚合 (已在聚合内完成状态变更)
     * @param fromStatus  变更前状态
     * @param eventSource 状态日志来源
     * @param note        状态日志备注 (可为空)
     * @return 更新后的订单聚合
     */
    @NotNull
    Order close(@NotNull Order order, @NotNull OrderStatus fromStatus,
                @NotNull OrderStatusEventSource eventSource, @Nullable String note);
}
