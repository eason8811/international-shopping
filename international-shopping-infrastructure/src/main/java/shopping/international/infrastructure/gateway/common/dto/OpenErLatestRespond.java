package shopping.international.infrastructure.gateway.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * open.er-api.com latest 响应体
 */
@Data
public class OpenErLatestRespond {
    /**
     * 响应结果
     */
    private String result;
    /**
     * 基准币种
     */
    private String baseCode;
    /**
     * 上次更新时间戳（Unix时间戳）
     */
    private Long timeLastUpdateUnix;
    /**
     * 上次更新时间（UTC时间）
     */
    private String timeLastUpdateUtc;
    /**
     * 错误类型
     */
    private String errorType;
    /**
     * 汇率数据
     */
    private Map<String, BigDecimal> rates;
}

