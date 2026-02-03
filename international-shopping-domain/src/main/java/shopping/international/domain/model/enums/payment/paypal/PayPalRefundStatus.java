package shopping.international.domain.model.enums.payment.paypal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * PayPal Refund Status
 *
 * <p>Ref: PayPal Payments v2 API - {@code refund.status}</p>
 *
 * <ul>
 *     <li>{@link #CANCELLED} 已取消</li>
 *     <li>{@link #FAILED} 失败</li>
 *     <li>{@link #PENDING} 正在处理</li>
 *     <li>{@link #COMPLETED} 已完成</li>
 *     <li>{@link #UNKNOWN} 未知</li>
 * </ul>
 */
public enum PayPalRefundStatus {
    /**
     * 已取消
     */
    CANCELLED,
    /**
     * 失败
     */
    FAILED,
    /**
     * 正在处理
     */
    PENDING,
    /**
     * 已完成
     */
    COMPLETED,
    /**
     * 未知
     */
    UNKNOWN;

    /**
     * 将 PayPal 返回的退款状态字符串转换为 {@link PayPalRefundStatus} 枚举类型
     *
     * @param raw 从 PayPal API 获取到的退款状态字符串, 可以为 null 或空白
     * @return 返回对应的 {@link PayPalRefundStatus} 枚举值, 如果输入为空或未知, 则返回 {@link #UNKNOWN}
     */
    public static @NotNull PayPalRefundStatus from(@Nullable String raw) {
        if (raw == null || raw.isBlank())
            return UNKNOWN;
        String s = raw.strip().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "CANCELLED", "CANCELED" -> CANCELLED;
            case "FAILED" -> FAILED;
            case "PENDING" -> PENDING;
            case "COMPLETED", "SUCCESS" -> COMPLETED;
            default -> UNKNOWN;
        };
    }
}
