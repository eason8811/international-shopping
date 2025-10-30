package shopping.international.api.resp.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.model.enums.user.AccountStatus;

import java.time.LocalDateTime;

/**
 * 用户账户概要响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountRespond {
    /**
     * 用户ID
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 邮箱 (可能为空)
     */
    private String email;
    /**
     * 手机 (可能为空)
     */
    private String phone;
    /**
     * 账户状态
     */
    private AccountStatus status;
    /**
     * 最近登录时间 (可能为空)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 从领域聚合 {@link User} 转换为响应对象
     *
     * @param user 领域聚合
     * @return 响应对象
     */
    public static UserAccountRespond from(User user) {
        if (user == null)
            return null;
        return new UserAccountRespond(
                user.getId(),
                user.getUsername() == null ? null : user.getUsername().getValue(),
                user.getNickname() == null ? null : user.getNickname().getValue(),
                user.getEmail() == null ? null : user.getEmail().getValue(),
                user.getPhone() == null ? null : user.getPhone().getValue(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
