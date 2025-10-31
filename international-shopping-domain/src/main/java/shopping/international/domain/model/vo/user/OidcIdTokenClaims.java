package shopping.international.domain.model.vo.user;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * 通过远端端口验签并解析的 ID Token 声明集 (仅保留我们关心的字段)
 *
 * @param iss           发行者
 * @param aud           受众 (通常包含 clientId)
 * @param sub           发行方内用户唯一 ID
 * @param exp           过期时间
 * @param iat           签发时间
 * @param nbf           生效时间 (可空)
 * @param nonce         授权阶段提供的 nonce (要求与缓存一致)
 * @param email         邮箱 (可空)
 * @param emailVerified 邮箱是否已校验 (可空)
 * @param name          显示名 (可空)
 * @param picture       头像 (可空)
 */
public record OidcIdTokenClaims(
        String iss,
        List<String> aud,
        String sub,
        Instant exp,
        Instant iat,
        @Nullable Instant nbf,
        @Nullable String nonce,
        @Nullable String email,
        @Nullable Boolean emailVerified,
        @Nullable String name,
        @Nullable String picture
) {
}
