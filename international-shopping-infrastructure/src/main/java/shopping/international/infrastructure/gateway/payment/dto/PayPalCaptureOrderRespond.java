package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

import java.time.OffsetDateTime;
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
    private List<PurchaseUnit> purchaseUnits;

    @Data
    public static class PurchaseUnit {
        private Payments payments;
    }

    @Data
    public static class Payments {
        private List<Capture> captures;
    }

    @Data
    public static class Capture {
        private String id;
        private String status;
        private OffsetDateTime createTime;
    }
}

