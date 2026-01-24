package shopping.international.api.resp.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;

/**
 * 创建 PayPal Checkout 响应体
 *
 * <p>用于向前端返回支付单信息与 PayPal 收银台跳转链接</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCheckoutRespond {

    /**
     * 支付单 ID (payment_order.id)
     */
    private Long paymentId;

    /**
     * 业务订单号
     */
    private String orderNo;

    /**
     * 支付通道 (通常为 PAYPAL)
     */
    private PaymentChannel channel;

    /**
     * 支付金额 (金额字符串，通常为 "主单位" 字符串表示)
     */
    private String amount;

    /**
     * 币种代码 (如 USD)
     */
    private String currency;

    /**
     * 支付单状态
     */
    private PaymentStatus status;

    /**
     * PayPal Order ID (落在 payment_order.external_id)
     */
    private String paypalOrderId;

    /**
     * PayPal 跳转链接 (前端跳转使用)
     */
    private String approveUrl;
}

