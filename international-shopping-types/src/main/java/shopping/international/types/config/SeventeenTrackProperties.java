package shopping.international.types.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 17Track 网关配置
 */
@Data
@ConfigurationProperties(prefix = "seventeen-track")
public class SeventeenTrackProperties {

    /**
     * 17Track API 基础地址
     */
    private String baseUrl;
    /**
     * 17Track API Token, 对应请求头 17token
     */
    private String token;
    /**
     * WebHook 验签密钥, 对应 17Track 推送配置中的 Webhook Key
     */
    private String webhookKey;
    /**
     * WebHook 重放保护 TTL
     */
    private Duration replayTtl;
}
