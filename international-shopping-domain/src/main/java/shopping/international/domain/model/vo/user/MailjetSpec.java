package shopping.international.domain.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>用于封装与Mailjet邮件服务相关的配置信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class MailjetSpec {
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
