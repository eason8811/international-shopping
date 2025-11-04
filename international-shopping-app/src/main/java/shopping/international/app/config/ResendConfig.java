package shopping.international.app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shopping.international.domain.model.vo.user.ResendSpec;

/**
 * <p>Resend 邮件服务配置类, 用于根据配置文件中的 Resend 相关设置创建 {@link ResendSpec} 实例, 该类通过启用 {@link ResendProperties} 来读取应用配置, 并将这些配置信息映射到领域模型对象中, 从而支持与 Resend 邮件服务的交互</p>
 *
 * <p>只有当配置文件中指定了 "mail.provider" 为 "resend" 时, 该配置类才会被激活</p>
 */
@Configuration
@EnableConfigurationProperties(ResendProperties.class)
@ConditionalOnProperty(prefix = "mail", name = "provider", havingValue = "resend")
public class ResendConfig {

    /**
     * 依据配置文件中的 Resend 邮件服务相关配置信息, 构建并返回一个 {@code ResendSpec} 对象
     *
     * @param properties 包含了 Resend 服务所需的所有配置属性, 如 baseUrl, apiKey, fromEmail, fromName 和 activationSubject 等
     * @return 返回构建好的 {@link ResendSpec} 对象, 包含了与 Resend 邮件服务交互所需的所有配置信息
     */
    @Bean
    public ResendSpec resendSpec(ResendProperties properties) {
        return ResendSpec.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .fromEmail(properties.getFromEmail())
                .fromName(properties.getFromName())
                .activationSubject(properties.getActivationSubject())
                .build();
    }
}
