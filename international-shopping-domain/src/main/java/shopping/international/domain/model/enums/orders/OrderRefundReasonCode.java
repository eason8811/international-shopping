package shopping.international.domain.model.enums.orders;

/**
 * 订单退款原因码
 * <ul>
 *     <li>{@code CUSTOMER_REQUEST} - 用户主动申请退款</li>
 *     <li>{@code RETURNED} - 已退回</li>
 *     <li>{@code LOST} - 丢失</li>
 *     <li>{@code DAMAGED} - 破损</li>
 *     <li>{@code PRICE_ADJUST} - 价格调整</li>
 *     <li>{@code DUPLICATE} - 重复下单</li>
 *     <li>{@code OTHER} - 其他</li>
 * </ul>
 */
public enum OrderRefundReasonCode {
    CUSTOMER_REQUEST,
    RETURNED,
    LOST,
    DAMAGED,
    PRICE_ADJUST,
    DUPLICATE,
    OTHER
}
