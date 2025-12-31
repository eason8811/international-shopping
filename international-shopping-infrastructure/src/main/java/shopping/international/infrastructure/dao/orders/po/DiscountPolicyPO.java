package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持久化对象: discount_policy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("discount_policy")
public class DiscountPolicyPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 策略名称
     */
    @TableField("name")
    private String name;
    /**
     * 作用域 (ORDER/ITEM)
     */
    @TableField("apply_scope")
    private String applyScope;
    /**
     * 折扣类型 (PERCENT/AMOUNT)
     */
    @TableField("strategy_type")
    private String strategyType;
    /**
     * 百分比折扣
     */
    @TableField("percent_off")
    private BigDecimal percentOff;
    /**
     * 固定金额折扣
     */
    @TableField("amount_off")
    private Long amountOff;
    /**
     * 币种 (可为空)
     */
    @TableField("currency")
    private String currency;
    /**
     * 门槛金额 (可为空)
     */
    @TableField("min_order_amount")
    private Long minOrderAmount;
    /**
     * 封顶金额 (可为空)
     */
    @TableField("max_discount_amount")
    private Long maxDiscountAmount;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
