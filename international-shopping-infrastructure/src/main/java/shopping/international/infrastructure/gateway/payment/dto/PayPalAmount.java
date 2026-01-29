package shopping.international.infrastructure.gateway.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayPal 金额对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalAmount {
    /**
     * 币种代码 (currency_code)
     */
    private String currencyCode;
    /**
     * 金额 (主单位字符串，如 {@code "12.99"})
     */
    private String value;
    /**
     * 明细
     */
    private Breakdown breakdown;

    /**
     * 明细
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Breakdown {
        /**
         * 商品总金额
         */
        private BreakdownItem itemTotal;
        /**
         * 运费
         */
        private BreakdownItem shipping;
        /**
         * 处理费
         */
        private BreakdownItem handling;
        /**
         * 税费总额
         */
        private BreakdownItem taxTotal;
        /**
         * 运费折扣
         */
        private BreakdownItem shippingDiscount;
        /**
         * 商品折扣
         */
        private BreakdownItem discount;
    }

    /**
     * 明细项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownItem {
        /**
         * 币种代码 (currency_code)
         */
        private String currencyCode;
        /**
         * 金额 (主单位字符串，如 {@code "12.99"})
         */
        private String value;
    }
}

