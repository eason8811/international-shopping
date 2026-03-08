package shopping.international.types.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Address Validation 配置
 */
@Data
@ConfigurationProperties(prefix = "google.address-validation")
public class GoogleAddressValidationProperties {

    /**
     * Google Address Validation API 基础地址
     */
    private String baseUrl;
    /**
     * Google Address Validation API Key
     */
    private String apiKey;
}
