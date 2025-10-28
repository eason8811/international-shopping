package shopping.international.domain.model.entity.user;

import lombok.*;
import lombok.experimental.Accessors;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.EncryptedSecret;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 认证映射实体 (对应表 {@code user_auth}), 用户的身份验证绑定信息, 归属 {@link User} 聚合
 *
 * @apiNote 在实体内不保存 {@code userId} 字段, 避免与聚合根重复, 持久化层映射时由聚合根的 {@code id} 参与
 */
@Getter
@ToString(exclude = {"accessToken", "refreshToken", "passwordHash"})
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AuthBinding {
    /**
     * 主键ID (可为 null, 表示尚未持久化)
     */
    private Long id;
    /**
     * 提供方
     *
     * @see AuthProvider
     */
    private AuthProvider provider;
    /**
     * 发行方 (OIDC iss), LOCAL 可为 null
     */
    private String issuer;
    /**
     * 提供方用户唯一ID (OIDC sub/openid), LOCAL 可为 null
     */
    private String providerUid;
    /**
     * 本地密码哈希 (仅 provider=LOCAL 有值)
     */
    private String passwordHash;
    /**
     * 访问令牌 (密文/加密后使用对象保存)
     *
     * @see EncryptedSecret
     */
    private EncryptedSecret accessToken;
    /**
     * 刷新令牌 (密文/加密后使用对象保存)
     *
     * @see EncryptedSecret
     */
    private EncryptedSecret refreshToken;
    /**
     * 访问令牌过期时间
     */
    private LocalDateTime expiresAt;
    /**
     * 授权范围
     */
    private String scope;
    /**
     * 该通道最近登录时间
     */
    private LocalDateTime lastLoginAt;
    /**
     * 创建时间 (快照)
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间 (快照)
     */
    private LocalDateTime updatedAt;

    /**
     * 创建一个基于本地认证的 {@link AuthBinding} 实例
     *
     * @param passwordHash 用户的密码哈希值, 不能为空或空白
     * @return 新创建的 {@link AuthBinding} 实例, 使用本地认证方式
     * @throws IllegalParamException 如果提供的 <code>passwordHash</code> 参数为空或仅包含空白字符
     */
    public static AuthBinding local(String passwordHash) {
        requireNotBlank(passwordHash, "密码哈希不能为空");
        return new AuthBinding(null, AuthProvider.LOCAL, null, null, passwordHash,
                null, null, null, null, null, null, null);
    }

    /**
     * 创建一个基于 OAuth 2.0 认证的 {@link AuthBinding} 实例
     *
     * @param provider    OAuth 2.0 提供方, 必须为有效的第三方认证提供方, 不能为 <code>null</code> 或 <code>AuthProvider.LOCAL</code>
     * @param issuer      发行者标识, 不能为空或空白
     * @param providerUid 提供方用户唯一标识, 不能为空或空白
     * @return 新创建的 {@link AuthBinding} 实例, 使用指定的 OAuth 2.0 认证方式
     * @throws IllegalParamException 如果提供的参数不符合要求
     */
    public static AuthBinding oauth(AuthProvider provider, String issuer, String providerUid) {
        require(provider != null && provider != AuthProvider.LOCAL, "提供方必须为 OAuth 2.0 提供方");
        requireNotBlank(issuer, "Issuer不能为空");
        requireNotBlank(providerUid, "ProviderUid不能为空");
        return new AuthBinding(null, provider, issuer, providerUid, null,
                null, null, null, null, null, null, null);
    }

    /**
     * 更新当前认证绑定的访问令牌、刷新令牌及其有效期和作用域
     *
     * @param accessToken  新的加密访问令牌, 不能为空
     * @param refreshToken 新的加密刷新令牌, 不能为空
     * @param expiresAt    访问令牌的有效期, 不能为空
     * @param scope        令牌的作用域, 不能为空或空白
     * @throws IllegalParamException 如果当前认证提供方为 LOCAL 或者提供的参数为空或不合法
     */
    public void updateTokens(EncryptedSecret accessToken, EncryptedSecret refreshToken, LocalDateTime expiresAt, String scope) {
        requireNotNull(accessToken, "访问令牌不能为空");
        requireNotNull(refreshToken, "刷新令牌不能为空");
        requireNotNull(expiresAt, "有效期不能为空");
        requireNotBlank(scope, "令牌作用域不能为空");
        require(provider != AuthProvider.LOCAL, "LOCAL 绑定无需更新令牌");
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.scope = scope;
    }

    /**
     * 记录用户的最后一次登录时间 (使用当前时间)
     */
    public void recordLogin() {
        recordLogin(null);
    }

    /**
     * 记录用户的最后一次登录时间
     *
     * @param now 用户登录的具体时间, 如果为 null 则使用当前时间
     */
    public void recordLogin(LocalDateTime now) {
        this.lastLoginAt = now == null ? LocalDateTime.now() : now;
    }

    /**
     * 修改当前认证绑定的本地密码哈希值。
     *
     * @param newPasswordHash 新的密码哈希值, 不能为空
     * @throws IllegalParamException 如果提供的 <code>newPasswordHash</code> 参数为空或仅包含空白字符,
     *                               或者当前认证提供方不是 <code>AuthProvider.LOCAL</code>
     */
    public void changeLocalPassword(String newPasswordHash) {
        requireNotNull(newPasswordHash, "密码哈希不能为空");
        require(provider == AuthProvider.LOCAL, "只有 LOCAL 绑定才有密码哈希");
        this.passwordHash = newPasswordHash;
    }
}
