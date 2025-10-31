package shopping.international.domain.adapter.port.user;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.user.*;

/**
 * OAuth2/OIDC 远程交互端口
 * <p>职责: 与第三方授权服务器交互: 置换 Token, 验签并解析 ID Token, 获取 UserInfo</p>
 */
public interface IOAuth2RemotePort {

    /**
     * 授权码置换 Token (支持 PKCE)
     *
     * @param spec          提供方配置
     * @param code          授权码
     * @param redirectUri   授权阶段使用的 redirect_uri (必须完全一致)
     * @param codeVerifier  授权阶段生成的 code_verifier
     * @return Token 响应体
     */
    @NotNull OAuth2TokenResponse exchangeAuthorizationCode(@NotNull OAuth2ProviderSpec spec, @NotNull String code,
                                                           @NotNull String redirectUri, @NotNull String codeVerifier);

    /**
     * 验签并解析 ID Token (OIDC)
     *
     * @param spec    提供方配置 (包含 issuer, aud/clientId, jwkSetUri 等)
     * @param idToken 原始 id_token 字符串
     * @return 解析后的声明集
     */
    @NotNull OidcIdTokenClaims verifyAndParseIdToken(@NotNull OAuth2ProviderSpec spec, @NotNull String idToken);

    /**
     * 使用 access_token 获取用户信息 (如果配置了 userinfo 端点)
     *
     * @param spec         提供方配置
     * @param accessToken  访问令牌
     * @return 用户信息 (可能为空)
     */
    @NotNull OidcUserInfo fetchUserInfo(@NotNull OAuth2ProviderSpec spec, @NotNull String accessToken);
}
