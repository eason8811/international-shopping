package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
     * 折扣策略按币种金额配置列表 (对应表 discount_policy_amount), 仅用于聚合查询回填
     */
    @TableField(exist = false)
    private List<DiscountPolicyAmountPO> amounts;
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
