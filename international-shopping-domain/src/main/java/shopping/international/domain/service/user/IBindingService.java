package shopping.international.domain.service.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.user.AuthBinding;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2CallbackResult;

import java.util.List;

/**
 * 第三方账号绑定/解绑 应用服务
 * <p>
 * 说明:
 * - 绑定发起阶段: 生成第三方授权URL并记录一次性state (携带BIND意图与userId)
 * - 回调阶段: 校验state并完成与当前用户的绑定 (不需要前端传authCode)
 * - 解绑阶段: 按provider (或你需要时可扩展为按bindingId)解除绑定
 */
public interface IBindingService {

    /**
     * 查询当前用户的绑定列表
     *
     * @param userId 当前登录用户ID
     * @return 用户的认证绑定列表, 包含所有与该用户关联的第三方认证信息, 每个 {@link AuthBinding} 对象代表一个认证绑定
     */
    @NotNull
    List<AuthBinding> listBindings(@NotNull Long userId);

    /**
     * 构建用于第三方账号绑定的授权 URL
     *
     * @param userId      用户ID, 用于标识当前用户
     * @param provider    第三方认证提供方, 指定要绑定的第三方服务
     * @param redirectUrl 绑定成功后的重定向 URL, 可选参数, 如果为空则使用默认值
     * @return 授权 URL 字符串, 用于引导用户前往第三方平台进行授权操作
     */
    @NotNull
    String buildBindAuthorizationUrl(@NotNull Long userId, @NotNull AuthProvider provider, @Nullable String redirectUrl);


    /**
     * 处理第三方账号绑定回调, 根据提供的参数校验并完成与当前用户的绑定
     *
     * @param provider         第三方认证提供方, 指定要绑定的第三方服务
     * @param code             授权码, 由第三方平台在授权成功后返回
     * @param state            状态标识, 用于防止跨站请求伪造攻击, 通常为生成授权URL时附带的一次性随机字符串
     * @param error            错误信息, 如果有错误发生则由第三方平台返回
     * @param errorDescription 错误描述, 提供更详细的错误信息
     * @return OAuth2CallbackResult 对象, 包含处理结果及可能的令牌信息和重定向URL, 详情见 {@link OAuth2CallbackResult}
     */
    @NotNull
    OAuth2CallbackResult handleBindCallback(@NotNull AuthProvider provider, @Nullable String code,
                                            @Nullable String state, @Nullable String error,
                                            @Nullable String errorDescription);

    /**
     * 解除指定用户与第三方认证提供方之间的绑定关系
     *
     * @param userId   用户ID, 指定要解绑的用户
     * @param provider 第三方认证提供方, 指定要解除绑定的服务
     */
    void unbind(@NotNull Long userId, @NotNull AuthProvider provider);
}
