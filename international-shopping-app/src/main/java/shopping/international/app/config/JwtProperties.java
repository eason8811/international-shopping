package shopping.international.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 相关配置项
 */
@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    /**
     * JWT 的 iss (发行者) 校验值, 可选但推荐
     */
    private String issuer;
    /**
     * JWT 的 aud (受众) 校验值, 可选
     */
    private String audience;
    /**
     * Base64 编码的 HMAC 密钥 (HS256 用)
     */
    private String secretBase64;
    /**
     * 允许的时钟偏移 (秒), 避免因服务器时钟微小偏差导致的误判
     */
    private long clockSkewSeconds = 60;
    /**
     * JWT 令牌过期时间 (秒)
     */
    private int accessTokenValiditySeconds;
    /**
     * JWT 刷新令牌过期时间 (秒)
     */
    private int refreshTokenValiditySeconds;
}
