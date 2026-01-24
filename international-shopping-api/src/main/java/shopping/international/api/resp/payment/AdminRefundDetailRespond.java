package shopping.international.api.resp.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.RefundStatus;

import java.time.LocalDateTime;

/**
 * 管理侧退款单详情响应体 (含 {@code request/response/notify} payload)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundDetailRespond {

    /**
     * 退款单 ID (payment_refund.id)
     */
    private Long refundId;

    /**
     * 退款业务单号 (refund_no)
     */
    private String refundNo;

    /**
     * 订单 ID (orders.id)
     */
    private Long orderId;

    /**
     * 业务订单号 (联表返回)
     */
    private String orderNo;

    /**
     * 关联支付单 ID (payment_order.id)
     */
    private Long paymentId;

    /**
     * 网关退款外部单号 (如 PayPal Refund ID)
     */
    @Nullable
    private String externalId;

    /**
     * 支付通道
     */
    private PaymentChannel channel;

    /**
     * 退款状态
     */
    private RefundStatus status;

    /**
     * 退款金额 (金额字符串)
     */
    private String amount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 退款请求报文 (JSON, 可为空)
     */
    @Nullable
    private JsonNode requestPayload;

    /**
     * 退款响应报文 (JSON, 可为空)
     */
    @Nullable
    private JsonNode responsePayload;

    /**
     * 最近一次回调报文 (JSON, 可为空)
     */
    @Nullable
    private JsonNode notifyPayload;

    /**
     * 最近轮询时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPolledAt;

    /**
     * 最近回调处理时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastNotifiedAt;

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
}

