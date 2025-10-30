package shopping.international.domain.model.vo.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * OAuth2 回调处理结果 (领域值对象)
 *
 * <p>用途: 承载第三方回调处理后的结果数据, 提供给控制器做后续响应</p>
 * <ul>
 *   <li>{@link #isSuccess()}: 是否处理成功</li>
 *   <li>{@link #getAccessToken()}: 成功时签发的本地访问令牌 (例如 JWT 字符串)</li>
 *   <li>{@link #getRefreshToken()}: 成功时签发的本地刷新令牌 (可能为 null, 取决于策略与 scope)</li>
 *   <li>{@link #getRedirectUrl()}: 控制器最终跳转的目标地址 (前端页)</li>
 * </ul>
 *
 * <p>说明: </p>
 * <ul>
 *   <li>该对象不涉及任何 Cookie/HTTP 语义, 控制器负责将字符串令牌写入 Cookie</li>
 *   <li>出错时 {@code success=false}, 可仍然返回一个前端错误展示页的 {@code redirectUrl}</li>
 * </ul>
 */
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class OAuth2CallbackResult {
    /**
     * 是否成功
     */
    private final boolean success;
    /**
     * 本地访问令牌 (成功时非空, 失败时为空)
     */
    @Nullable
    private final String accessToken;
    /**
     * 本地刷新令牌 (成功时可能存在, 失败或未发放时为空)
     */
    @Nullable
    private final String refreshToken;
    /**
     * 回调后前端要跳转到的地址 (成功或失败场景都可返回)
     */
    private final String redirectUrl;

    /**
     * 构造成功结果
     *
     * @param accessToken  本地访问令牌
     * @param refreshToken 本地刷新令牌 (可空)
     * @param redirectUrl  前端跳转地址
     * @return 成功结果
     */
    public static OAuth2CallbackResult success(String accessToken, @Nullable String refreshToken, String redirectUrl) {
        return new OAuth2CallbackResult(true, accessToken, refreshToken, redirectUrl);
    }

    /**
     * 构造失败结果
     *
     * @param redirectUrl 前端跳转地址 (例如错误提示页)
     * @return 失败结果
     */
    public static OAuth2CallbackResult failure(String redirectUrl) {
        return new OAuth2CallbackResult(false, null, null, redirectUrl);
    }
}
