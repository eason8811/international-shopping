package shopping.international.types.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * PayPal 网关对接配置
 */
@Data
@ConfigurationProperties(prefix = "paypal")
public class PayPalProperties {

    /**
     * PayPal API Base URL
     *
     * <p>示例:</p>
     * <ul>
     *     <li>{@code Sandbox:} <a href="https://api-m.sandbox.paypal.com">https://api-m.sandbox.paypal.com</a></li>
     *     <li>{@code Live:} <a href="https://api-m.paypal.com">https://api-m.sandbox.paypal.com</a></li>
     * </ul>
     */
    private String baseUrl;
    /**
     * PayPal Client ID
     */
    private String clientId;
    /**
     * PayPal Client Secret
     */
    private String clientSecret;
    /**
     * PayPal Webhook ID (用于 verify-webhook-signature)
     */
    private String webhookId;
    /**
     * Webhook 防重放 TTL (默认 1 天)
     */
    private Duration replayTtl;
    /**
     * Webhook 允许的时钟偏差 (默认 5 分钟)
     */
    private Duration clockSkew;
}

