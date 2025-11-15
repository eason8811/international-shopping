package shopping.international.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2ProviderSpec;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;

/**
 * <p>OAuth2 配置类, 用于根据配置文件中的 OAuth2 属性生成 {@link OAuth2ProviderSpec} 对象列表</p>
 *
 * @see OAuth2Properties
 */
@Configuration
@EnableConfigurationProperties(OAuth2Properties.class)
public class OAuth2Config {
    /**
     * 根据配置文件中的 OAuth2 属性生成 {@link OAuth2ProviderSpec} 列表
     *
     * @param properties 包含 OAuth2 配置信息的属性对象, 用于读取认证提供商的相关配置
     * @return 包含 {@link OAuth2ProviderSpec} 对象的列表, 每个对象代表一个认证提供商的配置快照
     * @throws IllegalParamException 如果没有配置任何认证提供商, 则抛出此异常
     */
    @Bean
    public List<OAuth2ProviderSpec> oAuth2ProviderSpecList(OAuth2Properties properties) {
        List<OAuth2Properties.AuthProviderProperties> providerPropertiesList = properties.getAuthProviderPropertiesList();
        if (providerPropertiesList == null || providerPropertiesList.isEmpty())
            throw new IllegalParamException("没有配置任何认证提供商");
        return providerPropertiesList.stream()
                .map(providerProperties -> {
                    AuthProvider provider = AuthProvider.valueOf(providerProperties.getProvider().toUpperCase());
                    return OAuth2ProviderSpec.builder()
                            .provider(provider)
                            .issuer(providerProperties.getIssuer())
                            .clientId(providerProperties.getClientId())
                            .clientSecret(providerProperties.getClientSecret())
                            .authorizationEndpoint(providerProperties.getAuthorizationEndpoint())
                            .tokenEndpoint(providerProperties.getTokenEndpoint())
                            .userinfoEndpoint(providerProperties.getUserinfoEndpoint())
                            .jwkSetUri(providerProperties.getJwkSetUri())
                            .loginRedirectUri(providerProperties.getLoginRedirectUri())
                            .bindRedirectUri(providerProperties.getBindRedirectUri())
                            .scope(providerProperties.getScope())
                            .defaultSuccessRedirect(providerProperties.getDefaultSuccessRedirect())
                            .clockSkewSeconds(providerProperties.getClockSkewSeconds())
                            .build();
                })
                .toList();
    }
}
