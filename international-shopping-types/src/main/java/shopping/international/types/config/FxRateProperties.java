package shopping.international.types.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FX 汇率相关配置
 */
@Data
@ConfigurationProperties(prefix = "fx.rate")
public class FxRateProperties {

    /**
     * 全站默认基准币种
     */
    private String baseCurrency = "USD";
}

