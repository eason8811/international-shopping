package shopping.international.domain.model.enums.payment;

/**
 * 支付单状态枚举
 *
 * <ul>
 *     <li><code>{@link #NONE}:</code> 占位/未开始</li>
 *     <li><code>{@link #INIT}:</code> 已初始化 (已创建支付单, 尚未进入网关待支付态)</li>
 *     <li><code>{@link #PENDING}:</code> 待支付 (网关侧已创建订单/等待用户完成支付)</li>
 *     <li><code>{@link #SUCCESS}:</code> 支付成功</li>
 *     <li><code>{@link #FAIL}:</code> 支付失败</li>
 *     <li><code>{@link #CLOSED}:</code> 支付单已关闭 (用户取消/换渠道/订单取消等)</li>
 *     <li><code>{@link #EXCEPTION}:</code> 异常 (晚到支付/履约不可用等, 需人工或自动补偿)</li>
 * </ul>
 */
public enum PaymentStatus {
    /**
     * 占位/未开始
     */
    NONE,
    /**
     * 已初始化
     */
    INIT,
    /**
     * 待支付
     */
    PENDING,
    /**
     * 支付成功
     */
    SUCCESS,
    /**
     * 支付失败
     */
    FAIL,
    /**
     * 已关闭
     */
    CLOSED,
    /**
     * 异常
     */
    EXCEPTION
}

