package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

/**
 * PayPal 查询 Refund 响应 (精简字段)
 */
@Data
public class PayPalGetRefundRespond {
    /**
     * PayPal Refund ID
     */
    private String id;
    /**
     * 状态
     */
    private String status;
}

