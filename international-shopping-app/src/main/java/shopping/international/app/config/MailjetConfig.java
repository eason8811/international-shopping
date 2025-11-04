package shopping.international.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shopping.international.domain.model.vo.user.MailjetSpec;

/**
 * <p>配置类, 用于初始化 Mailjet 相关的配置信息, 该类通过读取 application.yaml 文件中的 Mailjet 配置, 并将这些配置信息转换为 {@link MailjetSpec} 对象</p>
 */
@Configuration
@EnableConfigurationProperties(MailjetProperties.class)
public class MailjetConfig {
    /**
     * 依据配置文件中的 Mailjet 配置信息, 构建并返回一个 {@code MailjetSpec} 对象
     *
     * <p>该方法用于将应用层的配置映射为领域模型对象, 以便在服务中使用</p>
     *
     * @param mailjetProperties 包含了 Mailjet 相关配置信息的对象, 如 API 基础地址, API Key, API Secret, 发件人邮箱等
     * @return 返回构建好的 {@link MailjetSpec} 对象, 匎含了与Mailjet邮件服务相关的所有配置信息
     */
    @Bean
    public MailjetSpec mailjetSpec(MailjetProperties mailjetProperties) {
        return MailjetSpec.builder()
                .baseUrl(mailjetProperties.getBaseUrl())
                .apiKey(mailjetProperties.getApiKey())
                .apiSecret(mailjetProperties.getApiSecret())
                .fromEmail(mailjetProperties.getFromEmail())
                .fromName(mailjetProperties.getFromName())
                .activationSubject(mailjetProperties.getActivationSubject())
                .build();
    }
}
