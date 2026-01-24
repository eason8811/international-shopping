package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

/**
 * PayPal Refund Capture 响应
 */
@Data
public class PayPalRefundCaptureRespond {
    /**
     * PayPal Refund ID
     */
    private String id;
    /**
     * 状态
     */
    private String status;
}

