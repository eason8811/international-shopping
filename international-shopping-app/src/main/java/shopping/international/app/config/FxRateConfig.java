package shopping.international.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FX 配置项注册
 */
@Configuration
@EnableConfigurationProperties(shopping.international.types.config.FxRateProperties.class)
public class FxRateConfig {
}

