package shopping.international.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mailjet 发送邮件所需配置项
 *
 * <p>这些配置从 application.yaml 中读取: </p>
 * <ul>
 *     <li>{@code baseUrl}: Mailjet API 基础地址, 默认为 <a href="https://api.mailjet.com/v3.1">https://api.mailjet.com/v3.1</a>></li>
 *     <li>{@code apiKey}: Mailjet API Key</li>
 *     <li>{@code apiSecret}: Mailjet API Secret</li>
 *     <li>{@code fromEmail}: 发件人邮箱</li>
 *     <li>{@code fromName}: 发件人展示名</li>
 *     <li>{@code activationSubject}: 激活邮件主题 (可选, 默认 "Your verification code")</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "mailjet")
public class MailjetProperties {

    /**
     * Mailjet API 基础地址, 默认 v3.1
     * 示例: <a href="https://api.mailjet.com/v3.1">https://api.mailjet.com/v3.1</a>
     */
    private String baseUrl = "https://api.mailjet.com/v3.1";

    /**
     * Mailjet API Key
     */
    private String apiKey;

    /**
     * Mailjet API Secret
     */
    private String apiSecret;

    /**
     * 发件人邮箱地址
     */
    private String fromEmail;

    /**
     * 发件人显示名称
     */
    private String fromName;

    /**
     * 激活邮件主题, 可选
     */
    private String activationSubject = "Your verification code";
}
