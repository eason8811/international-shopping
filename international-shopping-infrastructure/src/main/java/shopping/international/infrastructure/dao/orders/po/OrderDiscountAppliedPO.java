package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: order_discount_applied
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("order_discount_applied")
public class OrderDiscountAppliedPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单 ID
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 订单明细 ID (可为空, 订单级折扣)
     */
    @TableField("order_item_id")
    private Long orderItemId;
    /**
     * 折扣码 ID
     */
    @TableField("discount_code_id")
    private Long discountCodeId;
    /**
     * 应用范围 (ORDER/ITEM)
     */
    @TableField("applied_scope")
    private String appliedScope;
    /**
     * 订单币种(冗余自 orders.currency)
     */
    @TableField("currency")
    private String currency;
    /**
     * 实际抵扣金额
     */
    @TableField("applied_amount")
    private Long appliedAmount;
    /**
     * 统一记账币种(全站默认)
     */
    @TableField("base_currency")
    private String baseCurrency;
    /**
     * 实际抵扣金额(统一记账币种,最小货币单位)
     */
    @TableField("applied_amount_base")
    private Long appliedAmountBase;
    /**
     * 折扣换算汇率快照(1 base = rate quote), base=base_currency, quote=currency
     */
    @TableField("fx_rate")
    private BigDecimal fxRate;
    /**
     * 汇率时间点/采样时间(快照)
     */
    @TableField("fx_as_of")
    private LocalDateTime fxAsOf;
    /**
     * 汇率数据源(快照)
     */
    @TableField("fx_provider")
    private String fxProvider;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
