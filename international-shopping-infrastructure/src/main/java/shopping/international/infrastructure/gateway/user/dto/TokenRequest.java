package shopping.international.infrastructure.gateway.user.dto;

import lombok.Builder;
import lombok.Singular;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Token 表单请求 DTO (用于构建 @FieldMap)
 */
@Builder
public class TokenRequest {
    /**
     * 授权请求常量字段
     */
    public static final String GRANT_TYPE_AUTH_CODE = "authorization_code";

    /**
     * 授权码
     */
    private final String code;
    /**
     * 与授权阶段一致的 redirect_uri
     */
    private final String redirectUri;
    /**
     * 客户端 ID
     */
    private final String clientId;
    /**
     * 客户端密钥 (公共客户端可空)
     */
    private final String clientSecret;
    /**
     * PKCE 明文
     */
    private final String codeVerifier;
    /**
     * 额外字段 (可选)
     */
    @Singular
    private final Map<String, String> extras;

    /**
     * 转为 Retrofit 的表单字段 Map
     */
    public Map<String, String> toFieldMap() {
        Map<String, String> map = new HashMap<>();
        map.put("grant_type", GRANT_TYPE_AUTH_CODE);
        map.put("code", code);
        map.put("redirect_uri", redirectUri);
        map.put("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank())
            map.put("client_secret", clientSecret);
        map.put("code_verifier", codeVerifier);
        if (extras != null)
            map.putAll(extras);
        return map;
    }
}
