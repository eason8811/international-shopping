package shopping.international.infrastructure.gateway.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PayPal 创建 Order 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCreateOrderRequest {
    /**
     * intent (通常为 CAPTURE)
     */
    private String intent;
    /**
     * purchase_units
     */
    private List<PurchaseUnit> purchaseUnits;
    /**
     * application_context
     */
    private ApplicationContext applicationContext;

    /**
     * purchase_unit
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseUnit {
        /**
         * amount
         */
        private PayPalAmount amount;
    }

    /**
     * application_context
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationContext {
        /**
         * return_url
         */
        private String returnUrl;
        /**
         * cancel_url
         */
        private String cancelUrl;
        /**
         * user_action (可选：PAY_NOW)
         */
        private String userAction;
    }
}

