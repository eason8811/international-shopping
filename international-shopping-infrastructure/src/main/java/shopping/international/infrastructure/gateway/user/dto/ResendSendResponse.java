package shopping.international.infrastructure.gateway.user.dto;

import lombok.Data;

/**
 * Resend 返回的结果
 */
@Data
public class ResendSendResponse {
    /**
     * Resend 返回的邮件ID
     */
    private String id;
}
