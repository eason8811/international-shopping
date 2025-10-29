package shopping.international.types.constant;

/**
 * 安全相关的常量约定
 *
 * <p>与 OpenAPI 文档保持一致：</p>
 * <ul>
 *   <li>会话 Cookie：{@code access_token}</li>
 *   <li>刷新 Cookie：{@code refresh_token}</li>
 *   <li>CSRF 双提交：Header {@code X-CSRF-Token} 与 Cookie {@code csrf_token}</li>
 * </ul>
 */
public final class SecurityConstants {

    /**
     * 私有构造函数, 不允许实例化
     */
    private SecurityConstants() {
    }

    /**
     * 访问令牌 (JWT) Cookie 名
     */
    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    /**
     * 刷新令牌 (JWT) Cookie 名
     */
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    /**
     * CSRF 令牌 Header 名
     */
    public static final String CSRF_HEADER = "X-CSRF-Token";

    /**
     * CSRF 令牌 Cookie 名
     */
    public static final String CSRF_COOKIE = "csrf_token";

    /**
     * API 版本前缀 (用于配置拦截路径)
     */
    public static final String API_PREFIX = "/api/v1";
}
