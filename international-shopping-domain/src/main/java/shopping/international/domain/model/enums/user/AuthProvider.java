package shopping.international.domain.model.enums.user;

import lombok.Getter;

/**
 * 第三方 / 本地认证通道, 与表 {@code user_auth.provider} 一致
 * <ul>
 *     <li>{@code LOCAL}: 本地认证</li>
 *     <li>{@code GOOGLE}: Google 认证</li>
 *     <li>{@code FACEBOOK}: Facebook 认证</li>
 *     <li>{@code APPLE}: Apple 认证</li>
 *     <li>{@code INSTAGRAM}: Instagram 认证</li>
 *     <li>{@code TIKTOK}: TikTok 认证</li>
 * </ul>
 */
@Getter
public enum AuthProvider {
    LOCAL, GOOGLE, FACEBOOK, APPLE, INSTAGRAM, TIKTOK
}
