package shopping.international.domain.model.enums.user;

import lombok.Getter;

/**
 * 第三方/本地认证通道，与表 user_auth.provider 一致。
 */
@Getter
public enum AuthProvider {
    LOCAL, GOOGLE, FACEBOOK, APPLE, INSTAGRAM, TIKTOK
}
