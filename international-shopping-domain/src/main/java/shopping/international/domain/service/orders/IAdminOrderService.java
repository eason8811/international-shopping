package shopping.international.domain.service.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.orders.Order;
import shopping.international.domain.model.entity.orders.InventoryLog;
import shopping.international.domain.model.entity.orders.OrderStatusLog;
import shopping.international.domain.model.enums.orders.*;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.AdminOrderSearchCriteria;
import shopping.international.domain.model.vo.orders.InventoryLogSearchCriteria;
import shopping.international.domain.model.vo.orders.OrderNo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 管理侧订单领域服务接口
 *
 * <p>覆盖管理侧订单管理能力:</p>
 * <ul>
 *     <li>订单列表筛选与详情</li>
 *     <li>订单取消、关单、确认退款</li>
 *     <li>订单状态日志与库存日志审计</li>
 * </ul>
 */
public interface IAdminOrderService {

    /**
     * 管理侧订单列表展示项
     *
     * <p>该类型用于管理侧订单列表页 (分页) 的读模型投影, 不承载聚合行为</p>
     *
     * @param id                订单 ID
     * @param orderNo           订单号
     * @param userId            用户 ID
     * @param status            订单状态
     * @param itemsCount        商品件数
     * @param totalAmountMinor       商品总额 (最小货币单位)
     * @param discountAmountMinor    折扣金额 (最小货币单位)
     * @param shippingAmountMinor    运费 (最小货币单位)
     * @param taxAmountMinor         税费 (最小货币单位)
     * @param payAmountMinor         应付金额 (最小货币单位)
     * @param currency          币种
     * @param payChannel        支付渠道
     * @param payStatus         支付状态
     * @param paymentExternalId 支付外部单号 (可为空)
     * @param payTime           支付时间 (可为空)
     * @param createdAt         创建时间
     * @param updatedAt         更新时间 (可为空)
     */
    record AdminOrderListItemView(Long id,
                                  String orderNo,
                                  Long userId,
                                  OrderStatus status,
                                  Integer itemsCount,
                                  long totalAmountMinor,
                                  long discountAmountMinor,
                                  long shippingAmountMinor,
                                  long taxAmountMinor,
                                  long payAmountMinor,
                                  String currency,
                                  PayChannel payChannel,
                                  PayStatus payStatus,
                                  @Nullable String paymentExternalId,
                                  @Nullable LocalDateTime payTime,
                                  LocalDateTime createdAt,
                                  @Nullable LocalDateTime updatedAt) {
    }

    /**
     * 查询订单列表 (管理侧)
     *
     * @param criteria 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @NotNull
    PageResult<AdminOrderListItemView> list(@NotNull AdminOrderSearchCriteria criteria, @NotNull PageQuery pageQuery);

    /**
     * 获取订单详情 (管理侧)
     *
     * @param orderNo 订单号
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<Order> getDetail(@NotNull OrderNo orderNo);

    /**
     * 取消订单 (管理侧)
     *
     * @param orderNo 订单号
     * @param reason  取消原因
     * @return 取消后的订单聚合
     */
    @NotNull
    Order cancel(@NotNull OrderNo orderNo, @NotNull String reason);

    /**
     * 列出指定时间前仍待支付的订单 (用于超时兜底)
     *
     * @param createdBefore 创建时间上限
     * @param limit         最大返回数量
     * @return 订单列表
     */
    @NotNull
    List<Order> listTimeoutCandidates(@NotNull LocalDateTime createdBefore, int limit);

    /**
     * 系统/调度侧取消未支付订单 (用于超时、兜底任务)
     *
     * @param orderNo 订单号
     * @param reason  取消原因
     * @param source  事件来源 (SYSTEM/SCHEDULER 等)
     */
    void cancelUnpaid(@NotNull OrderNo orderNo, @NotNull String reason, @NotNull OrderStatusEventSource source);

    /**
     * 关闭订单 (管理侧)
     *
     * @param orderNo 订单号
     * @param reason  关闭原因
     * @return 关闭后的订单聚合
     */
    @NotNull
    Order close(@NotNull OrderNo orderNo, @NotNull String reason);

    /**
     * 确认退款 (管理侧)
     *
     * @param orderNo 订单号
     * @param note    备注 (可为空)
     * @return 更新后的订单聚合
     */
    @NotNull
    Order confirmRefund(@NotNull OrderNo orderNo, @Nullable String note);

    /**
     * 查询订单状态流转日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @NotNull
    List<OrderStatusLog> listStatusLogs(@NotNull OrderNo orderNo);

    /**
     * 查询订单关联库存日志
     *
     * @param orderNo 订单号
     * @return 日志列表
     */
    @NotNull
    List<InventoryLog> listInventoryLogs(@NotNull OrderNo orderNo);

    /**
     * 分页查询库存日志
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @NotNull
    PageResult<InventoryLog> pageInventoryLogs(@NotNull InventoryLogSearchCriteria criteria, @NotNull PageQuery pageQuery);
}
