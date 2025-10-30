package shopping.international.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shopping.international.domain.model.vo.user.JwtIssueSpec;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtConfig {
    /**
     * JWT 相关配置项
     */
    private final JwtProperties jwtProperties;

    /**
     * 依据配置文件中的 JWT 配置信息, 构建并返回一个 {@code JwtIssueSpec} 对象
     *
     * <p>该方法用于将应用层的配置映射为领域模型对象, 以便在服务中使用</p>
     *
     * @return 返回构建好的 {@link JwtIssueSpec} 对象, 包含了 JWT 发行所需的所有配置信息
     */
    @Bean
    public JwtIssueSpec jwtIssueSpec() {
        return JwtIssueSpec.builder()
                .issuer(jwtProperties.getIssuer())
                .audience(jwtProperties.getAudience())
                .secretBase64(jwtProperties.getSecretBase64())
                .clockSkewSeconds(jwtProperties.getClockSkewSeconds())
                .accessTokenValiditySeconds(jwtProperties.getAccessTokenValiditySeconds())
                .refreshTokenValiditySeconds(jwtProperties.getRefreshTokenValiditySeconds())
                .build();
    }
}
