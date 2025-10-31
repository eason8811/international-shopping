// shopping.international.infrastructure.gateway.user.dto.UserInfoRespond
package shopping.international.infrastructure.gateway.user.dto;

import lombok.Data;

/**
 * OIDC UserInfo 响应 DTO (精简字段)
 *
 * <p>示例字段: {@code sub / email / email_verified / name / picture}</p>
 */
@Data
public class UserInfoRespond {
    /**
     * 主题 (可能是用户在该提供商的唯一标识)
     */
    private String sub;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 邮箱是否已验证
     */
    private Boolean emailVerified;
    /**
     * 用户名
     */
    private String name;
    /**
     * 用户头像 URL
     */
    private String picture;
}
