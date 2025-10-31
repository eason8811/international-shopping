package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2ProviderSpec;

/**
 * OAuth2/OIDC 提供方元信息端口
 * <p>职责: 按 {@link AuthProvider} 返回 clientId, 端点, scope, redirectUri, issuer 等配置</p>
 */
public interface IOAuth2MetaPort {

    /**
     * 加载指定提供方的元配置信息
     *
     * @param provider 第三方提供方
     * @return 提供方元信息
     */
    @NotNull OAuth2ProviderSpec getProviderSpec(@NotNull AuthProvider provider);
}
