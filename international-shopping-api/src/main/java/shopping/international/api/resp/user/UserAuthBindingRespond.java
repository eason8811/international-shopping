package shopping.international.api.resp.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AuthProvider;

import java.time.LocalDateTime;

/**
 * 认证绑定响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthBindingRespond {
    /**
     * 绑定ID
     */
    private Long id;
    /**
     * Provider
     */
    private AuthProvider provider;
    /**
     * Issuer (可空)
     */
    private String issuer;
    /**
     * Provider 内唯一ID (可空: LOCAL)
     */
    private String providerUid;
    /**
     * Scope (可空)
     */
    private String scope;
    /**
     * 该通道最近登录时间 (可空)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
    /**
     * 创建/更新
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 从 {@link AuthBinding} 对象创建一个 {@link UserAuthBindingRespond} 实例
     *
     * <p>该方法将 {@link AuthBinding} 对象中的信息转换为 {@link UserAuthBindingRespond} 对象, 以便于返回给客户端</p>
     *
     * @param binding 用于创建响应对象的认证绑定信息
     * @return 包含认证绑定信息的 {@link UserAuthBindingRespond} 实例
     */
    public static UserAuthBindingRespond from(AuthBinding binding) {
        return new UserAuthBindingRespond(
                binding.getId(),
                binding.getProvider(),
                binding.getIssuer(),
                binding.getProviderUid(),
                binding.getScope(),
                binding.getLastLoginAt(),
                binding.getCreatedAt(),
                binding.getUpdatedAt()
        );
    }
}
