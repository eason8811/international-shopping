package shopping.international.domain.model.enums.payment;

/**
 * 退款原因分类枚举 (对应表 payment_refund.reason_code)
 *
 * <ul>
 *     <li><code>{@link #CUSTOMER_REQUEST}:</code> 客户主动申请</li>
 *     <li><code>{@link #RETURNED}:</code> 已退货</li>
 *     <li><code>{@link #LOST}:</code> 丢失</li>
 *     <li><code>{@link #DAMAGED}:</code> 损坏</li>
 *     <li><code>{@link #PRICE_ADJUST}:</code> 价格调整</li>
 *     <li><code>{@link #DUPLICATE}:</code> 重复支付/重复退款等</li>
 *     <li><code>{@link #EXCEPTION}:</code> 支付单异常</li>
 *     <li><code>{@link #OTHER}:</code> 其他</li>
 * </ul>
 */
public enum RefundReasonCode {
    /**
     * 客户主动申请
     */
    CUSTOMER_REQUEST,
    /**
     * 已退货
     */
    RETURNED,
    /**
     * 丢失
     */
    LOST,
    /**
     * 损坏
     */
    DAMAGED,
    /**
     * 价格调整
     */
    PRICE_ADJUST,
    /**
     * 重复支付/重复退款等
     */
    DUPLICATE,
    /**
     * 支付单异常
     */
    EXCEPTION,
    /**
     * 其他
     */
    OTHER
}

