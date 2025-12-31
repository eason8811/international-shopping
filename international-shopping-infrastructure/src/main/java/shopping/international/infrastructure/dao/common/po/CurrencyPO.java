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
 * currency 表 PO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("currency")
public class CurrencyPO {
    @TableId(value = "code", type = IdType.INPUT)
    private String code;

    @TableField("minor_unit")
    private Integer minorUnit;

    @TableField("cash_rounding_inc")
    private BigDecimal cashRoundingInc;

    @TableField("rounding_mode")
    private String roundingMode;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

