package shopping.international.infrastructure.dao.user.po;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName("user_auth")
public class UserAuthPO {
    /**
     * 主键ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 提供方 (LOCAL/GOOGLE/...)
     */
    @TableField("provider")
    private String provider;
    /**
     * 发行方 (OIDC iss)
     */
    @TableField("issuer")
    private String issuer;
    /**
     * 发行方内唯一ID (OIDC sub)
     */
    @TableField("provider_uid")
    private String providerUid;
    /**
     * 本地密码哈希 (仅 LOCAL)
     */
    @TableField("password_hash")
    private String passwordHash;
    /**
     * 访问令牌 (加密后字节)
     */
    @TableField("access_token")
    private byte[] accessToken;
    /**
     * 刷新令牌 (加密后字节)
     */
    @TableField("refresh_token")
    private byte[] refreshToken;
    /**
     * 访问令牌过期时间
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    /**
     * 授权范围 (逗号或空格分隔)
     */
    @TableField("scope")
    private String scope;
    /**
     * 角色
     */
    @TableField("role")
    private String role;
    /**
     * 该通道最近登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
