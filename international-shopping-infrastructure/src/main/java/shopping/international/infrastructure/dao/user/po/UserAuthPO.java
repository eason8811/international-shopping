package shopping.international.infrastructure.dao.user.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: user_auth
 * <p>映射用户认证绑定 (本地/OAuth2)</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_auth")
public class UserAuthPO {
    /**
     * 主键ID (自增)
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;
    /**
     * 用户ID
     */
    @Column("user_id")
    private Long userId;
    /**
     * 提供方 (LOCAL/GOOGLE/...)
     */
    @Column("provider")
    private String provider;
    /**
     * 发行方 (OIDC iss)
     */
    @Column("issuer")
    private String issuer;
    /**
     * 发行方内唯一ID (OIDC sub)
     */
    @Column("provider_uid")
    private String providerUid;
    /**
     * 本地密码哈希 (仅 LOCAL)
     */
    @Column("password_hash")
    private String passwordHash;
    /**
     * 访问令牌 (加密后字节)
     */
    @Column("access_token")
    private byte[] accessToken;
    /**
     * 刷新令牌 (加密后字节)
     */
    @Column("refresh_token")
    private byte[] refreshToken;
    /**
     * 访问令牌过期时间
     */
    @Column("expires_at")
    private LocalDateTime expiresAt;
    /**
     * 授权范围 (逗号或空格分隔)
     */
    @Column("scope")
    private String scope;
    /**
     * 角色
     */
    @Column("role")
    private String role;
    /**
     * 该通道最近登录时间
     */
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
