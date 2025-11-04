package shopping.international.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "resend")
public class ResendProperties {
    /**
     * 形如 <a href="https://api.resend.com">https://api.resend.com</a>
     */
    private String baseUrl = "https://api.resend.com";
    /**
     * API Key
     */
    private String apiKey;
    /**
     * 发件邮箱
     */
    private String fromEmail;
    /**
     * 发件人名
     */
    private String fromName;
    /**
     * 主题
     */
    private String activationSubject = "Your verification code";
}
