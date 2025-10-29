package shopping.international.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import shopping.international.api.resp.Result;
import shopping.international.types.enums.ApiCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 层异常处理: 
 * <ul>
 *   <li>{@link AuthenticationEntryPoint}: 未认证访问的统一响应 (HTTP 401)</li>
 *   <li>{@link AccessDeniedHandler}: 已认证但权限不足的统一响应 (HTTP 403)</li>
 * </ul>
 *
 * <p>返回体遵循统一返回结构 {@link Result}, 避免与全局异常处理器重复响应格式</p>
 */
@RequiredArgsConstructor
public class RestAuthErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** JSON 序列化器 */
    private final ObjectMapper objectMapper;

    /**
     * 开始未认证访问的处理流程, 向客户端返回一个统一格式的 401 Unauthorized 响应
     *
     * @param request       HTTP 请求
     * @param response      HTTP 响应
     * @param authException 认证异常
     * @throws IOException 如果在写响应时发生 I/O 错误
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, Result.<Void>error(ApiCode.UNAUTHORIZED, "Unauthorized"));
    }

    /**
     * 处理已认证但权限不足的请求, 向客户端返回一个统一格式的 403 Forbidden 响应
     *
     * @param request             HTTP 请求
     * @param response            HTTP 响应
     * @param accessDeniedException 权限不足异常
     * @throws IOException 如果在写响应时发生 I/O 错误
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, Result.<Void>error(ApiCode.FORBIDDEN, "Access denied"));
    }

    /**
     * 将 {@link Result} 写出为 JSON
     *
     * @param response    HTTP 响应
     * @param status      HTTP 状态码
     * @param body        统一返回体
     * @throws IOException I/O 异常
     */
    private void write(HttpServletResponse response, int status, Result<Void> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
