package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PurchaseUnitItem {
    private Payments payments;

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
