package shopping.international.infrastructure.gateway.payment.dto;

import lombok.Data;

/**
 * PayPal OAuth2 Token 响应
 */
@Data
public class PayPalAccessTokenRespond {
    /**
     * access_token
     */
    private String accessToken;
    /**
     * token_type (通常为 Bearer)
     */
    private String tokenType;
    /**
     * 过期秒数
     */
    private Long expiresIn;
}

