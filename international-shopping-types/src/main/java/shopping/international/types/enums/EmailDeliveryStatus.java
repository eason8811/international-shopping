// shopping.international.types.enums.EmailDeliveryStatus
package shopping.international.types.enums;

/**
 * 邮件投递全量状态枚举, 覆盖 Resend 官方文档列举的所有事件
 * <p>注意：新增 UNKNOWN 作为兜底值, 避免遇到未识别状态时抛错</p>
 */
public enum EmailDeliveryStatus {

    /**
     * The recipient’s mail server rejected the email.
     */
    BOUNCED,
    /**
     * The scheduled email was canceled (by user).
     */
    CANCELED,
    /**
     * The recipient clicked on a link in the email.
     */
    CLICKED,
    /**
     * Delivered but recipient marked it as spam.
     */
    COMPLAINED,
    /**
     * Resend successfully delivered the email to recipient’s mail server.
     */
    DELIVERED,
    /**
     * Temporary issues delayed the delivery.
     */
    DELIVERY_DELAYED,
    /**
     * The email failed to be sent.
     */
    FAILED,
    /**
     * The recipient opened the email.
     */
    OPENED,
    /**
     * Email from Broadcasts or Batches is queued.
     */
    QUEUED,
    /**
     * Email is scheduled for delivery.
     */
    SCHEDULED,
    /**
     * The email was sent successfully.
     */
    SENT,
    /**
     * 未识别或暂不可用的状态
     */
    UNKNOWN;

    /**
     * 将字符串状态 (忽略大小写, 支持中划线)解析为枚举
     *
     * @param raw 原始状态字符串 (如 "delivery_delayed", "DELIVERED", "opened")
     * @return 对应的枚举值, 无法识别时返回 {@link #UNKNOWN}
     */
    public static EmailDeliveryStatus fromString(String raw) {
        if (raw == null || raw.isBlank()) 
            return UNKNOWN;
        String normalized = raw.trim().toUpperCase().replace('-', '_');
        try {
            return EmailDeliveryStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
