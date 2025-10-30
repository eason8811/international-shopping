package shopping.international.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import shopping.international.types.constant.SecurityConstants;

/**
 * 安全相关 Bean 声明
 *
 * <p>将 CsrfTokenRepository 作为 Bean 暴露, 便于在 SecurityConfig 与 Controller 里共用</p>
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * 注入 CSRF Token 仓库
     *
     * @param cookieProperties 包含 Cookie 安全性, SameSite 策略, 路径以及 HttpOnly 属性设置的配置对象
     * @return 配置好的 <code>CsrfTokenRepository</code> 实例, 用于在应用中启用和定制 CSRF 保护功能
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository(CookieProperties cookieProperties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(SecurityConstants.CSRF_COOKIE);
        repository.setHeaderName(SecurityConstants.CSRF_HEADER);
        repository.setParameterName(SecurityConstants.CSRF_HEADER);
        // 替代 setSecure/setCookiePath 的新写法：统一在自定义器里设置
        repository.setCookieCustomizer(builder -> builder
                .secure(cookieProperties.getSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .httpOnly(cookieProperties.getHttpOnly())
        );
        return repository;
    }
}
