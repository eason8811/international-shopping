package shopping.international.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import shopping.international.types.constant.SecurityConstants;

import java.io.IOException;
import java.util.Arrays;

/**
 * 基于 Cookie (HttpOnly) 携带的访问令牌 (JWT) 的认证过滤器
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>从请求 Cookie 中读取 {@code access_token}</li>
 *   <li>调用 {@link JwtTokenService#parseAndAuthenticate(String)} 验签并构造 {@link Authentication}</li>
 *   <li>若成功, 则设置到 {@link SecurityContextHolder}, 使后续授权链路生效</li>
 * </ol>
 *
 * <p>该过滤器是幂等的：若上下文已有认证, 则不重复解析</p>
 */
@Slf4j
@RequiredArgsConstructor
public class CookieJwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 令牌服务, 负责校验并构造 Authentication
     */
    private final JwtTokenService jwtTokenService;

    /**
     * 实现过滤器的核心逻辑, 用于处理基于 Cookie 的 JWT 认证
     *
     * <p>执行流程如下：</p>
     * <ol>
     *   <li>检查当前安全上下文中是否已有认证信息, 若有则直接放行</li>
     *   <li>从请求的 Cookie 中读取名为 {@code access_token} 的访问令牌</li>
     *   <li>通过 {@link JwtTokenService#parseAndAuthenticate(String)} 方法解析并验证该令牌</li>
     *   <li>若令牌有效, 则将其对应的认证信息设置到安全上下文中, 使后续授权链路生效</li>
     *   <li>若过程中发生任何异常, 则记录日志但不中断请求链路, 交由统一入口点处理未认证访问</li>
     * </ol>
     *
     * @param request  当前 HTTP 请求
     * @param response 用于响应客户端的 HTTP 响应
     * @param chain    过滤器链, 用于传递给下一个过滤器或目标资源
     * @throws ServletException 如果在处理过程中遇到 Servlet 相关的异常
     * @throws IOException      如果在处理过程中遇到 I/O 异常
     */
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                    @NotNull FilterChain chain) throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(request, response);
                return;
            }
            String raw = readCookie(request, SecurityConstants.ACCESS_TOKEN_COOKIE);
            if (raw == null || raw.isBlank()) {
                chain.doFilter(request, response);
                return;
            }
            Authentication auth = jwtTokenService.parseAndAuthenticate(raw);
            if (auth != null)
                SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            // 令牌异常不应中断整个链路, 交给 EntryPoint 统一处理未认证访问
            log.warn("[CookieJwtAuthentication] Token 转换异常: {}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }

    /**
     * 从请求中读取指定名称的 Cookie 值
     *
     * @param request 当前请求
     * @param name    Cookie 名
     * @return Cookie 值, 若不存在返回 {@code null}
     */
    @Nullable
    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
