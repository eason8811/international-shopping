package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: discount_policy_amount
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("discount_policy_amount")
public class DiscountPolicyAmountPO {

    /**
     * 折扣策略 ID
     */
    @TableId(value = "policy_id", type = IdType.INPUT)
    private Long policyId;
    /**
     * 币种
     */
    @TableField("currency")
    private String currency;
    /**
     * 固定减免金额 (最小货币单位), 可空
     */
    @TableField("amount_off")
    private Long amountOff;
    /**
     * 门槛金额 (最小货币单位), 可空
     */
    @TableField("min_order_amount")
    private Long minOrderAmount;
    /**
     * 封顶金额 (最小货币单位), 可空
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

