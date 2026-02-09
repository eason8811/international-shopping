package shopping.international.domain.model.enums.orders;

/**
 * 订单状态变更事件来源
 * <ul>
 *     <li>{@code SYSTEM} - 系统自动触发</li>
 *     <li>{@code USER} - 用户触发</li>
 *     <li>{@code PAYMENT_CALLBACK} - 支付回调触发</li>
 *     <li>{@code SCHEDULER} - 定时任务触发</li>
 *     <li>{@code ADMIN} - 管理后台触发</li>
 *     <li>{@code SHIPPING_CALLBACK} - 物流回调触发</li>
 * </ul>
 */
public enum OrderStatusEventSource {
    SYSTEM,
    USER,
    PAYMENT_CALLBACK,
    SCHEDULER,
    ADMIN,
    SHIPPING_CALLBACK
}
