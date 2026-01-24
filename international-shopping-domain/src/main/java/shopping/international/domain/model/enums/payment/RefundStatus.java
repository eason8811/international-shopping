package shopping.international.domain.model.enums.payment;

/**
 * 退款单状态枚举
 *
 * <ul>
 *     <li><code>{@link #INIT}:</code> 已创建退款请求 (尚未发往网关或尚未完成受理)</li>
 *     <li><code>{@link #PENDING}:</code> 网关处理中</li>
 *     <li><code>{@link #SUCCESS}:</code> 退款成功</li>
 *     <li><code>{@link #FAIL}:</code> 退款失败</li>
 *     <li><code>{@link #CLOSED}:</code> 退款关闭/撤销</li>
 *     <li><code>{@link #EXCEPTION}:</code> 异常 (对账/补偿场景扩展)</li>
 * </ul>
 */
public enum RefundStatus {
    /**
     * 初始化
     */
    INIT,
    /**
     * 处理中
     */
    PENDING,
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 失败
     */
    FAIL,
    /**
     * 已关闭
     */
    CLOSED,
    /**
     * 异常 (扩展态)
     */
    EXCEPTION
}

