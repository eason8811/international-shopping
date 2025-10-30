package shopping.international.trigger.controller.user;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.user.LoginRequest;
import shopping.international.api.req.user.RegisterRequest;
import shopping.international.api.req.user.ResendActivationRequest;
import shopping.international.api.req.user.VerifyEmailRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.user.CsrfTokenRespond;
import shopping.international.api.resp.user.UserAccountRespond;
import shopping.international.app.config.JwtProperties;
import shopping.international.domain.model.aggregate.user.User;
import shopping.international.domain.service.user.IAuthService;
import shopping.international.app.config.CookieProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import shopping.international.types.constant.SecurityConstants;

import java.time.Duration;

import static shopping.international.types.constant.SecurityConstants.API_PREFIX;

/**
 * 本地认证控制器
 *
 * <ul>
 *   <li>承接 OpenAPI: {@code /auth/register}, {@code /auth/verify-email}, {@code /auth/resend-activation}, {@code /auth/login}, {@code /auth/refresh-token}</li>
 *   <li>在控制器层完成 Cookie {@code access_token / refresh_token / csrf_token} 的下发与轮换</li>
 * </ul>
 */
@RestController
@RequestMapping(API_PREFIX + "/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * 领域层认证服务
     */
    private final IAuthService authService;
    /**
     * CSRF Token 仓库 (用于生成与保存 csrf_token Cookie)
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
     * 本地注册, 创建 DISABLED 账户并发送激活邮件
     *
     * @param req 注册请求体 (用户名, 密码哈希由应用服务处理, 昵称, 邮箱, 可选手机)
     * @return 202 Accepted 的统一返回体
     */
    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<Void>> register(@RequestBody RegisterRequest req) {
        req.validate();
        // 领域服务处理注册与发送激活邮件 (基础设施层后续用 SendGrid 实现)
        authService.register(req.getUsername(), req.getPassword(), req.getNickname(), req.getEmail(), req.getPhone());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Result.accepted("激活邮件已发送"));
    }

    /**
     * 验证邮箱验证码并激活账户: 激活后下发会话 Cookie 与 CSRF Cookie
     *
     * @param req      验证请求体 (邮箱 + 验证码)
     * @param request  当前请求 (用于 CSRF 令牌生成)
     * @param response 当前响应 (用于写 Set-Cookie)
     * @return 201 Created 的统一返回体, 携带用户账户概要
     */
    @PostMapping("/verify-email")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<UserAccountRespond>> verifyEmail(@RequestBody VerifyEmailRequest req,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response) {
        req.validate();
        // 激活账户并返回聚合快照
        User user = authService.verifyEmailAndActivate(req.getEmail(), req.getCode());

        // 下发会话 token (由应用/领域服务生成原始字符串, 控制器仅负责下发 Cookie)
        String accessToken = authService.issueAccessToken(user.getId());
        String refreshToken = authService.issueRefreshToken(user.getId());

        addCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, accessToken, true, jwtProperties.getAccessTokenValiditySeconds());
        addCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, refreshToken, true, jwtProperties.getRefreshTokenValiditySeconds());

        // 生成并保存 CSRF Token (与会话绑定)
        rotateAndSetCsrfCookie(request, response);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Result.created(UserAccountRespond.from(user)));
    }

    /**
     * 重新发送激活邮件
     *
     * @param req 含邮箱的请求体
     * @return 202 Accepted 的统一返回体
     */
    @PostMapping("/resend-activation")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<Void>> resendActivation(@RequestBody ResendActivationRequest req) {
        req.validate();
        authService.resendActivationEmail(req.getEmail());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Result.accepted("激活邮件已重新发送"));
    }

    /**
     * 本地登录: 校验成功后下发会话 Cookie 与 CSRF Cookie
     *
     * @param req      登录请求 (账号 + 密码)
     * @param request  当前请求 (用于 CSRF 令牌生成)
     * @param response 当前响应 (用于写 Set-Cookie)
     * @return 200 OK 的统一返回体, 携带用户账户概要
     */
    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<UserAccountRespond>> login(@RequestBody LoginRequest req,
                                                            HttpServletRequest request,
                                                            HttpServletResponse response) {
        req.validate();
        User user = authService.login(req.getAccount(), req.getPassword());

        String accessToken = authService.issueAccessToken(user.getId());
        String refreshToken = authService.issueRefreshToken(user.getId());

        addCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, accessToken, true, jwtProperties.getAccessTokenValiditySeconds());
        addCookie(response, SecurityConstants.REFRESH_TOKEN_COOKIE, refreshToken, true, jwtProperties.getRefreshTokenValiditySeconds());

        rotateAndSetCsrfCookie(request, response);

        return ResponseEntity.ok(Result.ok(UserAccountRespond.from(user), "登陆成功"));
    }

    /**
     * 刷新访问令牌: 读取刷新令牌 Cookie, 校验通过后下发新的 access_token (可选轮换 CSRF)
     *
     * <p>说明: 该接口通常不依赖现有 access_token 认证, 依赖 refresh_token + CSRF</p>
     *
     * @param request  当前请求 (用于可选 CSRF 轮换)
     * @param response 当前响应 (用于写 Set-Cookie)
     * @return 200 OK 的统一返回体 (不需数据)
     */
    @PostMapping("/refresh-token")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Result<Void>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refresh = readCookie(request, SecurityConstants.REFRESH_TOKEN_COOKIE);
        String newAccess = authService.refreshAccessToken(refresh);

        addCookie(response, SecurityConstants.ACCESS_TOKEN_COOKIE, newAccess, true, jwtProperties.getAccessTokenValiditySeconds());
        // 轮换 CSRF Token (与会话绑定)
        rotateAndSetCsrfCookie(request, response);

        return ResponseEntity.ok(Result.ok("Token 刷新成功"));
    }

    /**
     * 获取/轮换 CSRF 令牌: 返回并 Set-Cookie (与既有会话绑定)
     *
     * @param request  当前请求
     * @param response 当前响应 (用于写 Set-Cookie)
     * @return 200 OK 的统一返回体, 包含当前 CSRF 值
     */
    @GetMapping("/csrf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<CsrfTokenRespond>> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = rotateAndSetCsrfCookie(request, response);
        return ResponseEntity.ok(Result.ok(new CsrfTokenRespond(token.getToken()), "CSRF Token 已发布"));
    }

    // ================= 工具方法 (仅控制器内部使用)  =================

    /**
     * 生成并保存 CSRF Token, 同时下发 csrf_token Cookie
     *
     * @param request  当前请求
     * @param response 当前响应
     * @return 新的 {@link CsrfToken}
     */
    private CsrfToken rotateAndSetCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return token;
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

        if (ttlSeconds != null && ttlSeconds > 0)
            builder.maxAge(Duration.ofSeconds(ttlSeconds));
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    /**
     * 从请求中读取指定名称的 Cookie 值
     *
     * @param request 当前请求
     * @param name    Cookie 名称
     * @return Cookie 值或 null
     */
    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies())
            if (name.equals(cookie.getName()))
                return cookie.getValue();
        return null;
    }
}
