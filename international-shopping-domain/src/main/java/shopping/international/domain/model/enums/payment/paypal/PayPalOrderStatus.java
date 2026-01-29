package shopping.international.domain.model.enums.payment.paypal;

/**
 * PayPal 订单 order.status 枚举
 * <ul>
 *     <li>{@code CREATED:} 订单已创建</li>
 *     <li>{@code SAVED:} 订单已存储</li>
 *     <li>{@code APPROVED:} 订单已授权 (用户已经点击了支付按钮)</li>
 *     <li>{@code VOIDED:} 订单已作废 (需要将当前尝试状态置为 CLOSE)</li>
 *     <li>{@code COMPLETED:} 订单已完成</li>
 *     <li>{@code PAYER_ACTION_REQUIRED:} 需要支付方进一步动作</li>
 * </ul>
 */
public enum PayPalOrderStatus {
    /**
     * 订单已创建
     */
    CREATED,
    /**
     * 订单已存储
     */
    SAVED,
    /**
     * 订单已授权 (用户已经点击了支付按钮)
     */
    APPROVED,
    /**
     * 订单已作废 (需要将当前尝试状态置为 CLOSE)
     */
    VOIDED,
    /**
     * 订单已完成
     */
    COMPLETED,
    /**
     * 需要支付方进一步动作
     */
    PAYER_ACTION_REQUIRED
}
