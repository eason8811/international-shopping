package shopping.international.types.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 品牌相关配置
 */
@Data
@ConfigurationProperties(prefix = "brand-config")
public class BrandProperties {
    /**
     * 品牌名称
     */
    private String brand;
}
