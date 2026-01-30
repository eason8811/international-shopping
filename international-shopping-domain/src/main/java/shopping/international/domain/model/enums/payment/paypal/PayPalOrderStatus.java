package shopping.international.domain.model.enums.payment.paypal;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

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
    PAYER_ACTION_REQUIRED;

    /**
     * 尝试将 PayPal 返回的 order.status 映射为枚举
     * @param status 若为空/空白或出现未知值, 返回 null (由上层按 "未知/不可判定" 处理)
     * @return 返回 {@link PayPalOrderStatus} 对象
     */
    public static @Nullable PayPalOrderStatus tryParse(@Nullable String status) {
        if (status == null || status.isBlank())
            return null;
        String normalized = status.strip().toUpperCase(Locale.ROOT);
        try {
            return PayPalOrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }
}
