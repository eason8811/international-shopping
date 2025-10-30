package shopping.international.app.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import shopping.international.api.resp.Result;
import shopping.international.types.constant.SecurityConstants;

import java.io.IOException;
import java.time.Duration;

/**
 * 登出成功处理器: 清除会话相关 Cookie, 并返回统一结构
 *
 * <p>清除的 Cookie 包括: </p>
 * <ul>
 *   <li>{@code access_token}（HttpOnly）</li>
 *   <li>{@code csrf_token}（非 HttpOnly）</li>
 * </ul>
 */
@RequiredArgsConstructor
public class RestLogoutSuccessHandler implements LogoutSuccessHandler {

    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // 通过 Set-Cookie 覆盖为过期清空
        ResponseCookie clearAccess = ResponseCookie.from(SecurityConstants.ACCESS_TOKEN_COOKIE, "")
                .maxAge(Duration.ZERO).path("/").httpOnly(true).secure(true).sameSite("Lax").build();
        ResponseCookie clearCsrf = ResponseCookie.from(SecurityConstants.CSRF_COOKIE, "")
                .maxAge(Duration.ZERO).path("/").httpOnly(false).secure(true).sameSite("Lax").build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearCsrf.toString());

        // 统一返回结构
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), Result.<Void>ok("Logged out"));
    }
}
