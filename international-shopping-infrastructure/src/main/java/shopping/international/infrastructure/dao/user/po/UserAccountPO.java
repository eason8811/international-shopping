package shopping.international.infrastructure.dao.user.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 持久化对象: user_account
 * <p>映射用户账户主表 (JWT 认证), 用于 MyBatis-Plus CRUD</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_account")
public class UserAccountPO {
    /**
     * 主键ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 用户名(登录名)
     */
    @TableField("username")
    private String username;
    /**
     * 昵称/显示名
     */
    @TableField("nickname")
    private String nickname;
    /**
     * 邮箱
     */
    @TableField("email")
    private String email;
    /**
     * 手机号国家码 (E.164, 不含 '+')
     */
    @TableField("phone_country_code")
    private String phoneCountryCode;
    /**
     * 手机号 national number (E.164, 国家码之后的 National Significant Number, 仅数字)
     */
    @TableField("phone_national_number")
    private String phoneNationalNumber;
    /**
     * 手机号 E.164 字符串 (由 phone_country_code + phone_national_number 生成, 只读)
     */
    @TableField(value = "phone_e164", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String phoneE164;
    /**
     * 账户状态 (ACTIVE/DISABLED)
     */
    @TableField("status")
    private String status;
    /**
     * 最近登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    /**
     * 软删除标记(0/1)
     */
    @TableField("is_deleted")
    private Boolean isDeleted;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
