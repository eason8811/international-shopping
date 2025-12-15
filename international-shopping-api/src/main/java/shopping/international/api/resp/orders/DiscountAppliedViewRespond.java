package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;

import java.time.LocalDateTime;

/**
 * 折扣应用明细响应 (DiscountAppliedViewRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountAppliedViewRespond {
    /**
     * 折扣码 ID
     */
    private Long discountCodeId;
    /**
     * 订单明细 ID (可为空, 明细级折扣时使用)
     */
    private Long orderItemId;
    /**
     * 应用范围
     */
    private DiscountApplyScope appliedScope;
    /**
     * 实际抵扣金额 (金额字符串)
     */
    private String appliedAmount;
    /**
     * 发生时间 (可为空)
     */
    private LocalDateTime createdAt;
}

