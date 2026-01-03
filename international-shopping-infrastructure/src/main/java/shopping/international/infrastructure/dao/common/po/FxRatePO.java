package shopping.international.infrastructure.dao.common.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: fx_rate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fx_rate")
public class FxRatePO {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 基准币种(如 USD), 指向 currency.code
     */
    @TableField("base_code")
    private String baseCode;
    /**
     * 报价币种(如 EUR), 指向 currency.code
     */
    @TableField("quote_code")
    private String quoteCode;
    /**
     * 1 base = rate quote
     */
    @TableField("rate")
    private BigDecimal rate;
    /**
     * 汇率时间点/采样时间
     */
    @TableField("as_of")
    private LocalDateTime asOf;
    /**
     * 数据源(如 ECB / OpenExchangeRates)
     */
    @TableField("provider")
    private String provider;
    /**
     * 写入时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

