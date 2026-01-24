package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

import java.util.List;

/**
 * PayPal 创建 Order 响应
 */
@Data
public class PayPalCreateOrderRespond {
    /**
     * PayPal Order ID
     */
    private String id;
    /**
     * 状态
     */
    private String status;
    /**
     * links
     */
    private List<PayPalLink> links;
}

