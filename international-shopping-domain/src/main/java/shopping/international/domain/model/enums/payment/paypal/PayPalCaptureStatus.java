package shopping.international.domain.model.enums.payment.paypal;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * PayPal 订单 capture.status 枚举
 *
 * <ul>
 *     <li>{@code COMPLETED:} 扣款完成, 资金入账到收款方账户</li>
 *     <li>{@code DECLINED:} 扣款被拒绝</li>
 *     <li>{@code PARTIALLY_REFUNDED:} 已部分退款</li>
 *     <li>{@code PENDING:} 待处理, 未最终完成</li>
 *     <li>{@code REFUNDED:} 已全额退款</li>
 *     <li>{@code FAILED:} 扣款失败</li>
 * </ul>
 */
public enum PayPalCaptureStatus {
    /**
     * 扣款完成, 资金入账到收款方账户
     */
    COMPLETED,
    /**
     * 扣款被拒绝
     */
    DECLINED,
    /**
     * 已部分退款
     */
    PARTIALLY_REFUNDED,
    /**
     * 待处理, 未最终完成
     */
    PENDING,
    /**
     * 已全额退款
     */
    REFUNDED,
    /**
     * 扣款失败
     */
    FAILED;

    /**
     * 尝试将 PayPal 返回的 captures[].status 映射为枚举
     *
     * @param status 若为空/空白或出现未知值, 返回 null (由上层按 "未知/不可判定" 处理)
     * @return 返回 {@link PayPalOrderStatus} 对象
     */
    public static @Nullable PayPalCaptureStatus tryParse(@Nullable String status) {
        if (status == null || status.isBlank())
            return null;
        String normalized = status.strip().toUpperCase(Locale.ROOT);
        try {
            return PayPalCaptureStatus.valueOf(normalized);
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }
}
