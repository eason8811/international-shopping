package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.RefundStatus;

import java.time.LocalDateTime;

/**
 * 管理侧退款单详情 (读模型)
 *
 * @param refundId        退款单 ID
 * @param refundNo        退款单号
 * @param orderId         退款单关联的订单 ID
 * @param orderNo         退款单关联的订单号
 * @param paymentId       支付单 ID
 * @param externalId      退款单外部 ID
 * @param channel         支付渠道
 * @param status          退款单状态
 * @param amountMinor     退款单价格 (Minor 形式)
 * @param currency        币种
 * @param requestPayload  退款请求报文
 * @param responsePayload 退款响应报文
 * @param notifyPayload   最近一次回调报文
 * @param lastPolledAt    最近轮询时间
 * @param lastNotifiedAt  最近回调处理时间
 * @param createdAt       创建时间
 * @param updatedAt       更新时间
 */
public record AdminRefundDetail(@NotNull Long refundId,
                                @NotNull String refundNo,
                                @NotNull Long orderId,
                                @NotNull String orderNo,
                                @NotNull Long paymentId,
                                @Nullable String externalId,
                                @NotNull PaymentChannel channel,
                                @NotNull RefundStatus status,
                                long amountMinor,
                                @NotNull String currency,
                                @Nullable String requestPayload,
                                @Nullable String responsePayload,
                                @Nullable String notifyPayload,
                                @Nullable LocalDateTime lastPolledAt,
                                @Nullable LocalDateTime lastNotifiedAt,
                                @NotNull LocalDateTime createdAt,
                                @NotNull LocalDateTime updatedAt) {
}
