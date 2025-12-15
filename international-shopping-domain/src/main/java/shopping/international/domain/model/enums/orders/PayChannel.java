package shopping.international.domain.model.enums.orders;

/**
 * 支付渠道
 * <ul>
 *     <li>{@code NONE} - 无支付渠道/未选择</li>
 *     <li>{@code ALIPAY} - 支付宝</li>
 *     <li>{@code WECHAT} - 微信支付</li>
 *     <li>{@code STRIPE} - Stripe</li>
 *     <li>{@code PAYPAL} - PayPal</li>
 *     <li>{@code OTHER} - 其他渠道</li>
 * </ul>
 */
public enum PayChannel {
    NONE,
    ALIPAY,
    WECHAT,
    STRIPE,
    PAYPAL,
    OTHER
}
