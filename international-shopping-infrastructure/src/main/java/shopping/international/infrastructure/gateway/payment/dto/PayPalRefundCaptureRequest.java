package shopping.international.infrastructure.gateway.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayPal Refund Capture 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalRefundCaptureRequest {
    /**
     * amount
     */
    private PayPalAmount amount;
    /**
     * note_to_payer（可选）
     */
    private String noteToPayer;
}

