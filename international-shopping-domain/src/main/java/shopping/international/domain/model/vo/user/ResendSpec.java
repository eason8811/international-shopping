package shopping.international.domain.model.vo.user;

import lombok.Builder;
import lombok.Data;

/**
 * <p>用于封装与 Resend 邮件服务相关的配置信息</p>
 */
@Data
@Builder
public final class ResendSpec {
    /**
     * 形如 <a href="https://api.resend.com">https://api.resend.com</a>
     */
    private String baseUrl;
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
    private String activationSubject;
}
