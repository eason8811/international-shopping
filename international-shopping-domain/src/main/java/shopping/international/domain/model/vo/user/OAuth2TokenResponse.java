package shopping.international.domain.model.vo.user;

import org.jetbrains.annotations.Nullable;

/**
 * OAuth2 Token 端点返回结果的领域快照
 *
 * @param accessToken      访问令牌
 * @param expiresInSeconds 访问令牌有效期 (秒)
 * @param idToken          OIDC ID Token (可空)
 * @param refreshToken     刷新令牌 (可空)
 * @param scope            实际授予的 scope (可空)
 * @param tokenType        令牌类型 (如 Bearer)
 */
public record OAuth2TokenResponse(
        String accessToken,
        @Nullable Long expiresInSeconds,
        @Nullable String idToken,
        @Nullable String refreshToken,
        @Nullable String scope,
        @Nullable String tokenType
) {
}
