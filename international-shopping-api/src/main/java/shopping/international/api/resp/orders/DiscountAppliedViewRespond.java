package shopping.international.api.resp.orders;

import com.fasterxml.jackson.annotation.JsonFormat;
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
     * 所属订单号
     */
    private String orderNo;
    /**
     * 订单明细 ID (可为空, 明细级折扣时使用)
     */
    private Long orderItemId;
    /**
     * 折扣码 ID
     */
    private Long discountCodeId;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

