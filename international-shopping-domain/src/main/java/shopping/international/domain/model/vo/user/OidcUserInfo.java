package shopping.international.domain.model.vo.user;

import org.jetbrains.annotations.Nullable;

/**
 * OIDC UserInfo 端点返回的精简快照。
 *
 * @param sub           发行方内用户唯一 ID
 * @param email         邮箱 (可空)
 * @param emailVerified 邮箱是否已校验 (可空)
 * @param name          显示名 (可空)
 * @param avatar        头像 (可空)
 */
public record OidcUserInfo(
        String sub,
        @Nullable String email,
        @Nullable Boolean emailVerified,
        @Nullable String name,
        @Nullable String avatar
) {
}
