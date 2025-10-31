package shopping.international.infrastructure.adapter.port.user;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import shopping.international.app.config.OAuth2Properties;
import shopping.international.domain.adapter.port.user.IOAuth2MetaPort;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2ProviderSpec;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * OAuth2/OIDC 提供方元信息装配实现类
 * <p>职责: 按 {@link AuthProvider} 返回 clientId, 端点, scope, redirectUri, issuer 等配置</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2Properties.class)
public class OAuth2MetaPort implements IOAuth2MetaPort {
    /**
     * OAuth2 提供方元配置属性
     */
    private final OAuth2Properties props;

    /**
     * 加载指定提供方的元配置信息
     *
     * @param provider 第三方提供方
     * @return 提供方元信息
     */
    @Override
    @NotNull public OAuth2ProviderSpec getProviderSpec(@NotNull AuthProvider provider) {
        OAuth2Properties.AuthProviderProperties prop = props.getAuthProviderPropertiesList().stream()
                .filter(p -> p.getProvider().equals(provider.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalParamException("未找到提供方为" + provider.name() + "的元配置信息"));
        return new OAuth2ProviderSpec(
                provider,
                prop.getIssuer(),
                prop.getClientId(),
                prop.getClientSecret(),
                prop.getAuthorizationEndpoint(),
                prop.getTokenEndpoint(),
                prop.getUserinfoEndpoint(),
                prop.getJwkSetUri(),
                prop.getRedirectUri(),
                prop.getScope(),
                prop.getDefaultSuccessRedirect(),
                prop.getClockSkewSeconds()
        );
    }
}
