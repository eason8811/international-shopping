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
 * 持久化对象: user_account
 * <p>映射用户账户主表 (JWT 认证), 用于 MyBatis-Flex CRUD</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_account")
public class UserAccountPO {
    /**
     * 主键ID (自增)
     */
    @Id(keyType = KeyType.Auto)
    @Column("id")
    private Long id;
    /**
     * 用户名(登录名)
     */
    @Column("username")
    private String username;
    /**
     * 昵称/显示名
     */
    @Column("nickname")
    private String nickname;
    /**
     * 邮箱
     */
    @Column("email")
    private String email;
    /**
     * 手机号
     */
    @Column("phone")
    private String phone;
    /**
     * 账户状态 (ACTIVE/DISABLED)
     */
    @Column("status")
    private String status;
    /**
     * 最近登录时间
     */
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    /**
     * 软删除标记(0/1)
     */
    @Column("is_deleted")
    private Boolean isDeleted;
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
