package shopping.international.domain.model.enums.payment;

/**
 * 退款发起方枚举 (对应表 payment_refund.initiator)
 *
 * <ul>
 *     <li><code>{@link #USER}:</code> 用户发起</li>
 *     <li><code>{@link #ADMIN}:</code> 管理员发起</li>
 *     <li><code>{@link #SYSTEM}:</code> 系统自动发起</li>
 *     <li><code>{@link #SCHEDULER}:</code> 定时任务发起</li>
 * </ul>
 */
public enum RefundInitiator {
    /**
     * 用户发起
     */
    USER,
    /**
     * 管理员发起
     */
    ADMIN,
    /**
     * 系统自动发起
     */
    SYSTEM,
    /**
     * 定时任务发起
     */
    SCHEDULER
}

