package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

/**
 * PayPal Webhook 验签响应
 */
@Data
public class PayPalVerifyWebhookSignatureRespond {
    /**
     * verification_status（SUCCESS/FAILURE）
     */
    private String verificationStatus;
}

