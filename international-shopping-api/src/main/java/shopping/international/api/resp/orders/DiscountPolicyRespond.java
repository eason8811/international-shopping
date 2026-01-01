package shopping.international.api.resp.orders;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;

import java.time.LocalDateTime;
import java.util.List;

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
     * 币种金额配置列表
     */
    private List<DiscountPolicyAmountRespond> amounts;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 折扣策略币种金额配置响应 (DiscountPolicyAmountRespond)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountPolicyAmountRespond {
        /**
         * 币种
         */
        private String currency;
        /**
         * 固定减免金额 (可为空, 金额字符串)
         */
        private String amountOff;
        /**
         * 最小订单金额限制 (可为空, 金额字符串)
         */
        private String minOrderAmount;
        /**
         * 最大折扣金额上限 (可为空, 金额字符串)
         */
        private String maxDiscountAmount;
    }
}
