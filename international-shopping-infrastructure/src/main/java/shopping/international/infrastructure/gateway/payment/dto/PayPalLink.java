package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

/**
 * PayPal 超链接对象
 */
@Data
public class PayPalLink {
    /**
     * href
     */
    private String href;
    /**
     * rel
     */
    private String rel;
    /**
     * method
     */
    private String method;
}

