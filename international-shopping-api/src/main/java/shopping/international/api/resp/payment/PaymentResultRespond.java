package shopping.international.api.resp.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentStatus;

/**
 * 支付结果响应体 (用于 capture/cancel 等结果返回)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultRespond {

    /**
     * 支付单 ID (payment_order.id)
     */
    private Long paymentId;

    /**
     * 支付单状态
     */
    private PaymentStatus status;

    /**
     * 支付网关外部单号 (如 PayPal Order ID), 可为空
     */
    @Nullable
    private String externalId;

    /**
     * 业务订单号 (可选回传)
     */
    @Nullable
    private String orderNo;

    /**
     * 结果说明 (可选)
     */
    @Nullable
    private String message;
}

