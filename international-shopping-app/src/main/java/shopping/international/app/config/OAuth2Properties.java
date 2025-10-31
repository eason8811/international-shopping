package shopping.international.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * OAuth 2.0 相关配置
 */
@Data
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {
    /**
     * 认证提供商列表
     */
    private List<AuthProviderProperties> authProviderPropertiesList;

    /**
     * 认证提供商配置
     */
    @Data
    public static final class AuthProviderProperties {
        /**
         * 认证提供商名称
         */
        private String provider;
        /**
         * 认证提供商地址
         */
        private String issuer;
        /**
         * 客户端 ID
         */
        private String clientId;
        /**
         * 客户端密钥
         */
        private String clientSecret;
        /**
         * 认证接口
         */
        private String authorizationEndpoint;
        /**
         * 令牌接口
         */
        private String tokenEndpoint;
        /**
         * 用户信息接口
         */
        private String userinfoEndpoint;
        /**
         * JWK 地址
         */
        private String jwkSetUri;
        /**
         * 重定向地址
         */
        private String redirectUri;
        /**
         * 授权范围
         */
        private String scope;
        /**
         * 默认成功重定向地址
         */
        private String defaultSuccessRedirect;
        /**
         * 可容忍时钟偏移量 (秒)
         */
        private long clockSkewSeconds;
    }
}
