package shopping.international.trigger.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
     * 获取或轮换 CSRF 令牌
     *
     * <p>此方法用于生成新的 CSRF 令牌并通过 HTTP 响应将其写入客户端的 Cookie 中, 同时, 该方法会将新生成的令牌以 JSON 格式返回给客户端</p>
     *
     * @param request HTTP 请求 (用于获取当前请求上下文)
     * @param response HTTP 响应 (用于设置包含 CSRF 令牌的 Cookie)
     * @return 返回一个 {@link Result} 对象, 其中包含一个键为 "csrfToken" 的 Map, 存储着当前有效的 CSRF 令牌值
     */
    @GetMapping(SecurityConstants.API_PREFIX + "/auth/csrf")
    public Result<Map<String, String>> issue(HttpServletRequest request, HttpServletResponse response) {
        // 生成并下发 (实际写 Cookie 由 repo 完成)
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);

        // 仅回显 token, 无需再次手动 Set-Cookie (避免重复写入)
        return Result.ok(Map.of("csrfToken", token.getToken()), "CSRF token 已发布");
    }
}
