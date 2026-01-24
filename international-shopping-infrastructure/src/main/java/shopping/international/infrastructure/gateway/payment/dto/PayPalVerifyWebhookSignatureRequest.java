package shopping.international.infrastructure.gateway.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * PayPal Webhook 验签请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalVerifyWebhookSignatureRequest {
    /**
     * auth_algo
     */
    private String authAlgo;
    /**
     * cert_url
     */
    private String certUrl;
    /**
     * transmission_id
     */
    private String transmissionId;
    /**
     * transmission_sig
     */
    private String transmissionSig;
    /**
     * transmission_time
     */
    private String transmissionTime;
    /**
     * webhook_id
     */
    private String webhookId;
    /**
     * webhook_event
     */
    private Map<String, Object> webhookEvent;
}

