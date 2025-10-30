package shopping.international.domain.model.vo.user;

import lombok.Builder;
import org.jetbrains.annotations.Nullable;

/**
 * JWT 发行配置
 *
 * <p>该对象由上层 (应用层) 在装配领域服务时提供, 通常来源于配置文件,
 * 但为了保持分层纯净, 不直接依赖应用层的配置类</p>
 *
 * @param issuer                      JWT 的发行者 (iss), 可选但推荐设置
 * @param audience                    JWT 的受众 (aud), 可选
 * @param secretBase64                Base64 编码后的 HMAC 密钥 (HS256)
 * @param accessTokenValiditySeconds  访问令牌过期时间 (秒)
 * @param refreshTokenValiditySeconds 刷新令牌过期时间 (秒)
 * @param clockSkewSeconds            允许的时钟偏移 (秒), 用于校验 nbf/exp 的容错
 */
@Builder
public record JwtIssueSpec(@Nullable String issuer, @Nullable String audience, String secretBase64,
                           int accessTokenValiditySeconds, int refreshTokenValiditySeconds, long clockSkewSeconds) {
}
