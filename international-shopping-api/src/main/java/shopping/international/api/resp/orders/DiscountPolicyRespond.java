package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;

import java.time.LocalDateTime;

/**
 * 折扣策略响应 (DiscountPolicyRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicyRespond {
    /**
     * 折扣策略 ID
     */
    private Long id;
    /**
     * 策略名称
     */
    private String name;
    /**
     * 应用范围
     */
    private DiscountApplyScope applyScope;
    /**
     * 策略类型
     */
    private DiscountStrategyType strategyType;
    /**
     * 折扣百分比 (可为空)
     */
    private Double percentOff;
    /**
     * 折扣金额 (可为空, 金额字符串)
     */
    private String amountOff;
    /**
     * 币种 (可为空)
     */
    private String currency;
    /**
     * 最小订单金额限制 (可为空, 金额字符串)
     */
    private String minOrderAmount;
    /**
     * 最大折扣金额上限 (可为空, 金额字符串)
     */
    private String maxDiscountAmount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

