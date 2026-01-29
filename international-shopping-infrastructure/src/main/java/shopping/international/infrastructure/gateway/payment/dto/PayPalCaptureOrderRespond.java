package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

import java.util.List;

/**
 * PayPal capture Order 响应
 */
@Data
public class PayPalCaptureOrderRespond {
    /**
     * PayPal Order ID
     */
    private String id;
    /**
     * 状态
     */
    private String status;
    /**
     * purchase_units
     */
    private List<PurchaseUnitItem> purchaseUnits;
}

