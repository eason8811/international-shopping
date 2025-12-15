package shopping.international.domain.model.enums.orders;

/**
 * 订单状态
 * <ul>
 *     <li>{@code CREATED} - 已创建</li>
 *     <li>{@code PENDING_PAYMENT} - 待支付</li>
 *     <li>{@code PAID} - 已支付</li>
 *     <li>{@code CANCELLED} - 已取消</li>
 *     <li>{@code CLOSED} - 已关闭</li>
 *     <li>{@code FULFILLED} - 已履约完成</li>
 *     <li>{@code REFUNDING} - 退款中</li>
 *     <li>{@code REFUNDED} - 已退款</li>
 * </ul>
 */
public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    CLOSED,
    FULFILLED,
    REFUNDING,
    REFUNDED
}
