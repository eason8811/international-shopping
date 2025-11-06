package shopping.international.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import shopping.international.app.security.service.IJwtTokenService;
import shopping.international.app.security.filter.CookieJwtAuthenticationFilter;
import shopping.international.app.security.handler.RestAuthErrorHandlers;
import shopping.international.app.security.handler.RestLogoutSuccessHandler;

import static shopping.international.types.constant.SecurityConstants.API_PREFIX;

/**
 * Spring Security 主配置
 *
 * <p>能力: </p>
 * <ul>
 *   <li>基于 Cookie (HttpOnly) 承载的 JWT 访问令牌进行无状态认证</li>
 *   <li>双提交 CSRF (Header {@code X-CSRF-Token} 与 Cookie {@code csrf_token})</li>
 *   <li>匿名白名单: OpenAPI 中声明为 security:[] 的端点</li>
 *   <li>统一未认证/授权失败处理</li>
 *   <li>退出登录时清除 Cookie</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(CookieProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * JWT 令牌服务
     */
    private final IJwtTokenService jwtTokenService;
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper;
    /**
     * CSRF Token 仓库
     */
    private final CsrfTokenRepository csrfRepo;

    /**
     * 定义 Spring Security 的过滤链
     *
     * @param http HttpSecurity 配置器
     * @return 过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 匿名入口 (无需 CSRF 的接口) ——使用字符串重载, 避免 AntPathRequestMatcher
        String[] csrfIgnoring = new String[]{
                // Auth: 匿名入口
                API_PREFIX + "/auth/register",
                API_PREFIX + "/auth/email-status",
                API_PREFIX + "/auth/verify-email",
                API_PREFIX + "/auth/resend-activation",
                API_PREFIX + "/auth/login",
                // OAuth2: 匿名入口
                API_PREFIX + "/oauth2/*/authorize",
                API_PREFIX + "/oauth2/*/callback",
                // 刷新端点 (可按策略决定是否忽略) 
                API_PREFIX + "/auth/refresh-token"
        };

        http.csrf(csrf -> configureCsrf(csrf, csrfRepo, csrfIgnoring));

        // ========== 无状态会话 (JWT)  ==========
        http.sessionManagement(sessionManager ->
                sessionManager.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // ========== 认证入口/拒绝处理 ==========
        RestAuthErrorHandlers authErrors = new RestAuthErrorHandlers(objectMapper);
        http.exceptionHandling(handling -> handling
                .authenticationEntryPoint(authErrors)
                .accessDeniedHandler(authErrors)
        );

        // ========== 授权规则 ==========
        http.authorizeHttpRequests(registry ->
                        // 匿名接口允许访问
                {
                    registry.requestMatchers(HttpMethod.GET).permitAll();
                    registry.requestMatchers(HttpMethod.HEAD).permitAll();
                    registry.requestMatchers(HttpMethod.TRACE).permitAll();
                    registry.requestMatchers(HttpMethod.OPTIONS).permitAll();
                    registry.requestMatchers(
                                    API_PREFIX + "/auth/register",
                                    API_PREFIX + "/auth/email-status",
                                    API_PREFIX + "/auth/verify-email",
                                    API_PREFIX + "/auth/resend-activation",
                                    API_PREFIX + "/auth/login",
                                    API_PREFIX + "/oauth2/*/authorize",
                                    API_PREFIX + "/oauth2/*/callback"
                            ).permitAll()
                            // 其余默认需要认证
                            .anyRequest().authenticated();
                }
        );

        // ========== JWT Cookie 认证过滤器 ==========
        http.addFilterBefore(new CookieJwtAuthenticationFilter(jwtTokenService), UsernamePasswordAuthenticationFilter.class);

        // ========== 登出 ==========
        http.logout(logout -> logout
                .logoutUrl(API_PREFIX + "/auth/logout")
                .logoutSuccessHandler(new RestLogoutSuccessHandler(objectMapper))
        );

        // ========== 其它常规项 (可按需开启)  ==========
        http.cors(Customizer.withDefaults());
        http.headers(header -> header.cacheControl(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * 配置 CSRF (双提交):
     * <ul>
     *   <li>令牌存于 Cookie {@code csrf_token} (非 HttpOnly)</li>
     *   <li>客户端提交 Header {@code X-CSRF-Token}</li>
     *   <li>对匿名入口接口关闭 CSRF 检查</li>
     *   <li>对所有写操作 (POST/PUT/PATCH/DELETE) 强制校验</li>
     * </ul>
     *
     * @param csrf             CSRF 配置器
     * @param repo             Cookie CSRF 仓库
     * @param ignoringPatterns 需要忽略 CSRF 的路径模式 (字符串模式)
     */
    private void configureCsrf(CsrfConfigurer<HttpSecurity> csrf, CsrfTokenRepository repo, String... ignoringPatterns) {
        csrf.csrfTokenRepository(repo)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(ignoringPatterns);
        // 默认仅对写操作启用检查, GET/HEAD/TRACE/OPTIONS 不校验
    }
}
