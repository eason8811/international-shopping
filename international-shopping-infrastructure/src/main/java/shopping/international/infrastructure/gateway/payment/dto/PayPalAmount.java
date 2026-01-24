package shopping.international.infrastructure.gateway.payment.dto;

import lombok.AllArgsConstructor;
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
}

