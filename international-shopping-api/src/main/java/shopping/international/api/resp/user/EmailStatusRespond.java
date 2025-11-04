package shopping.international.api.resp.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.enums.EmailDeliveryStatus;

/**
 * 邮件状态查询响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailStatusRespond {
    /**
     * 查询邮箱
     */
    private String email;
    /**
     * Resend 邮件 ID
     */
    private String messageId;
    /**
     * 当前投递状态
     */
    private EmailDeliveryStatus status;
}
