package shopping.international.domain.model.vo.user;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.user.AuthProvider;

/**
 * OAuth2/OIDC 提供方配置快照 (从配置文件/DB 读取, 经 MetaPort 暴露给领域层)
 *
 * @param provider               提供方枚举
 * @param issuer                 发行者 (OIDC 校验使用；非 OIDC 也可配置)
 * @param clientId               客户端 ID
 * @param clientSecret           客户端密钥 (公共客户端可空)
 * @param authorizationEndpoint  授权端点
 * @param tokenEndpoint          置换 token 端点
 * @param userinfoEndpoint       用户信息端点 (可空)
 * @param jwkSetUri              JWK 集合地址 (验签使用；若 remotePort 内部缓存/信任库也可空)
 * @param redirectUri            授权阶段与回调阶段使用的 redirect_uri (必须一致)
 * @param scope                  授权范围 (空格分隔, 例如 "openid email profile")
 * @param defaultSuccessRedirect 本地登录完成默认前端落地页 (当未携带 redirect 时使用)
 * @param clockSkewSeconds       允许的时钟偏移 (验签时间窗口)
 */
@Builder
public record OAuth2ProviderSpec(
        AuthProvider provider,
        @Nullable String issuer,
        String clientId,
        @Nullable String clientSecret,
        String authorizationEndpoint,
        String tokenEndpoint,
        @Nullable String userinfoEndpoint,
        @Nullable String jwkSetUri,
        String redirectUri,
        String scope,
        @Nullable String defaultSuccessRedirect,
        long clockSkewSeconds
) {
}
