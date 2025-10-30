package shopping.international.trigger.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.RedirectUrlRespond;
import shopping.international.app.config.JwtProperties;
import shopping.international.domain.model.enums.user.AuthProvider;
import shopping.international.domain.model.vo.user.OAuth2CallbackResult;
import shopping.international.domain.service.user.IOAuth2Service;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.app.config.CookieProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

/**
 * 第三方登录 (OAuth2/OIDC) 控制器
 *
 * <p>职责: </p>
 * <ul>
 *   <li>生成授权地址并重定向 {@code /oauth2/{provider}/authorize}</li>
 *   <li>处理回调 {@code /oauth2/{provider}/callback}, 成功时下发会话与 CSRF Cookie, 并重定向到前端页面</li>
 * </ul>
 */
@RestController
@RequestMapping(SecurityConstants.API_PREFIX + "/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    /**
     * 第三方登录应用服务 (仅接口, 具体实现稍后提供)
     */
    private final IOAuth2Service oAuth2Service;
    /**
     * CSRF Token 仓库 (用于回调成功后生成与保存 csrf_token Cookie)
     */
    private final CsrfTokenRepository csrfTokenRepository;
    /**
     * Cookie 安全属性配置
     */
    private final CookieProperties cookieProperties;
    /**
     * JWT 安全属性配置
     */
    private final JwtProperties jwtProperties;

    /**
     * 生成授权页 URL 并 (由前端) 发起跳转
     *
     * @param provider    第三方提供方
     * @param redirectUrl 成功后的站内跳转地址
     * @return 302 Found + 统一返回体 (data.url)
     */
    @GetMapping("/{provider}/authorize")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<RedirectUrlRespond>> authorize(@PathVariable("provider") AuthProvider provider,
                                                                @RequestParam(value = "redirectUrl", required = false) String redirectUrl) {
        String url = oAuth2Service.buildAuthorizationUrl(provider, redirectUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Result.found(new RedirectUrlRespond(url)));
    }

    /**
     * 第三方回调: 成功时下发会话与 CSRF Cookie, 并 302 重定向到前端
     *
     * @param provider         第三方提供方
     * @param code             授权码 (成功场景)
     * @param state            状态码 (防 CSRF)
     * @param error            错误码 (失败场景)
     * @param errorDescription 错误描述
     * @param request          当前请求 (用于 CSRF 生成)
     * @param response         当前响应 (用于写 Set-Cookie)
     * @return 302 Found + 统一返回体 (data.url)
     */
    @GetMapping("/{provider}/callback")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<RedirectUrlRespond>> callback(@PathVariable("provider") AuthProvider provider,
                                                               @RequestParam(value = "code", required = false) String code,
                                                               @RequestParam(value = "state", required = false) String state,
                                                               @RequestParam(value = "error", required = false) String error,
                                                               @RequestParam(value = "error_description", required = false) String errorDescription,
                                                               HttpServletRequest request,
                                                               HttpServletResponse response) {
        // 应用服务完成 state 校验, 换 token, 绑定/注册用户并返回: 会话令牌, 前端重定向地址
        OAuth2CallbackResult result = oAuth2Service.handleCallback(provider, code, state, error, errorDescription);

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.of(
                            false,
                            shopping.international.types.enums.ApiCode.BAD_REQUEST,
                            "非法的 state 或认证提供方",
                            new RedirectUrlRespond(result.getRedirectUrl()),
                            null
                    ));
        }

        // 下发会话 Cookie
        addCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, result.getAccessToken(), true, jwtProperties.getAccessTokenValiditySeconds());
        addCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, result.getRefreshToken(), true, jwtProperties.getRefreshTokenValiditySeconds());

        // 轮换 CSRF
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);

        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Result.found(new RedirectUrlRespond(result.getRedirectUrl())));
    }

    /**
     * 向响应中添加一个 Cookie
     *
     * @param response   用于设置 Cookie 的 {@link HttpServletResponse} 对象
     * @param name       Cookie 的名称
     * @param value      Cookie 的值 如果为 null, 则使用空字符串代替
     * @param httpOnly   是否设置 HttpOnly 属性 如果为 {@code true}, 那么该 Cookie 将不能通过客户端脚本访问
     * @param ttlSeconds Cookie 的生存时间 单位是秒 如果为 null 或者小于等于 0, 则不设置最大年龄
     */
    private void addCookie(HttpServletResponse response, String name, String value, boolean httpOnly, Integer ttlSeconds) {
        if (value == null || value.isEmpty())
            return;
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(cookieProperties.getSecure())
                .path(cookieProperties.getPath())
                .sameSite(cookieProperties.getSameSite());

        if (ttlSeconds != null && ttlSeconds > 0) {
            builder.maxAge(Duration.ofSeconds(ttlSeconds));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
