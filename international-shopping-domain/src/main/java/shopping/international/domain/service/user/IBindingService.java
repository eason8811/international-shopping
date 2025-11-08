package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AuthProvider;

import java.util.List;

/**
 * 认证绑定领域服务接口
 *
 * <p>职责: 
 * <ul>
 *   <li>绑定列表</li>
 *   <li>基于授权码的绑定 (具体交换逻辑延迟到实现)</li>
 *   <li>解绑 (校验不允许解绑最后方式)</li>
 * </ul>
 * </p>
 */
public interface IBindingService {

    /**
     * 列出当前用户的绑定
     */
    @NotNull
    List<AuthBinding> listBindings(@NotNull Long userId);

    /**
     * 使用授权码绑定 (不同 Provider 的交换细节由实现处理)
     *
     * @param userId   用户ID
     * @param provider 提供方
     * @param authCode 授权码 (可空: 若实现支持其他路径)
     * @return 新增的绑定
     */
    @NotNull
    AuthBinding bindByAuthCode(@NotNull Long userId, @NotNull AuthProvider provider, @Nullable String authCode);

    /**
     * 解绑
     *
     * @param userId   用户ID
     * @param provider 提供方
     */
    void unbind(@NotNull Long userId, @NotNull AuthProvider provider);
}
