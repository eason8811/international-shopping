package shopping.international.trigger.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import shopping.international.api.resp.Result;
import shopping.international.types.constant.SecurityConstants;

import java.util.Map;

/**
 * CSRF 令牌下发/轮换接口
 *
 * <p>约定: </p>
 * <ul>
 *   <li>返回体: 统一格式, data 内回显 {@code csrfToken}</li>
 *   <li>同时 Set-Cookie: {@code csrf_token} (非 HttpOnly, Secure, SameSite=Lax)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class CsrfController {

    /**
     * 与 SecurityConfig 中保持同一个 CsrfTokenRepository 实例
     */
    private final CsrfTokenRepository csrfTokenRepository;

    /**
     * 获取/轮换 CSRF 令牌
     *
     * @param response HTTP 响应 (用于写入 Set-Cookie)
     * @return 统一返回, 其中 {@code data.csrfToken} 为当前有效令牌
     */
    @GetMapping(SecurityConstants.API_PREFIX + "/auth/csrf")
    public Result<Map<String, String>> issue(HttpServletResponse response) {
        // 生成并下发 (此处不依赖 HttpSession)
        CsrfToken token = csrfTokenRepository.generateToken(null);
        csrfTokenRepository.saveToken(token, null, response);

        // 加强 Cookie 属性: SameSite、Secure 已在 CookieCsrfTokenRepository 配置时处理
        ResponseCookie echo = ResponseCookie.from(SecurityConstants.CSRF_COOKIE, token.getToken())
                .httpOnly(false).secure(true).path("/").sameSite("Lax").build();
        response.addHeader("Set-Cookie", echo.toString());

        return Result.ok(Map.of("csrfToken", token.getToken()), "CSRF token 已发布");
    }
}
