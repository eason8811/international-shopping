package shopping.international.infrastructure.gateway.user.dto;

import lombok.Data;

/**
 * OAuth2 Token 响应 DTO (与 Google 等返回字段对齐)
 *
 * <p>示例字段: {@code access_token / expires_in / id_token / refresh_token / scope / token_type}</p>
 */
@Data
public class TokenRespond {
    /**
     * 访问用户信息 Token
     */
    private String accessToken;
    /**
     * 过期时间 (秒)
     */
    private Long expiresIn;
    /**
     * OIDC ID Token
     */
    private String idToken;
    /**
     * 刷新访问用户信息 Token 的刷新 Token
     */
    private String refreshToken;
    /**
     * 可访问的权限范围
     */
    private String scope;
    /**
     * Token 类型
     */
    private String tokenType;
}
