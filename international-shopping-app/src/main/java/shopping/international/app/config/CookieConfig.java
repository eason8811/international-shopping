package shopping.international.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shopping.international.domain.model.vo.user.CookieSpec;

/**
 * <p>Cookie 配置类, 用于根据配置文件中的 Cookie 相关设置创建 {@link CookieSpec} 实例, 该类通过启用 {@link CookieProperties} 来读取应用配置, 并将这些配置信息映射到领域模型对象中, 从而支持更灵活的 Cookie 操作</p>
 */
@Configuration
@EnableConfigurationProperties(CookieProperties.class)
public class CookieConfig {
    /**
     * 依据配置文件中的 Cookie 配置信息, 构建并返回一个 {@code CookieSpec} 对象
     *
     * @param cookieProperties 包含了 Cookie 相关的所有配置属性, 如 secure, sameSite, path 和 httpOnly 等
     * @return 返回构建好的 {@link CookieSpec} 对象, 包含了 Cookie 的所有配置信息, 用于后续的 Cookie 操作
     */
    @Bean
    public CookieSpec cookieSpec(CookieProperties cookieProperties) {
        return CookieSpec.builder()
                .secure(cookieProperties.getSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .httpOnly(cookieProperties.getHttpOnly())
                .build();
    }
}
