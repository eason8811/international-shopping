package shopping.international.domain.model.enums.payment;

/**
 * 支付通道枚举
 *
 * <ul>
 *     <li><code>{@link #NONE}:</code> 占位/未选择通道</li>
 *     <li><code>{@link #ALIPAY}:</code> 支付宝</li>
 *     <li><code>{@link #WECHAT}:</code> 微信支付</li>
 *     <li><code>{@link #STRIPE}:</code> Stripe</li>
 *     <li><code>{@link #PAYPAL}:</code> PayPal</li>
 *     <li><code>{@link #OTHER}:</code> 其他通道</li>
 * </ul>
 */
public enum PaymentChannel {
    /**
     * 占位/未选择通道
     */
    NONE,
    /**
     * 支付宝
     */
    ALIPAY,
    /**
     * 微信支付
     */
    WECHAT,
    /**
     * Stripe
     */
    STRIPE,
    /**
     * PayPal
     */
    PAYPAL,
    /**
     * 其他通道
     */
    OTHER
}

