package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 管理侧支付单详情 (读模型)
 *
 * @param paymentId       支付单 ID
 * @param orderId         订单 ID
 * @param orderNo         订单号
 * @param externalId      支付单外部 ID
 * @param channel         支付渠道
 * @param status          支付单状态
 * @param amountMinor     支付单价格 (Minor 形式)
 * @param currency        币种
 * @param requestPayload  下单请求报文
 * @param responsePayload 下单响应报文
 * @param notifyPayload   最近一次回调报文(JSON)
 * @param lastPolledAt    最近轮询时间
 * @param lastNotifiedAt  最近回调处理时间
 * @param createdAt       创建时间
 * @param updatedAt       更新时间
 */
public record AdminPaymentDetail(@NotNull Long paymentId,
                                 @NotNull Long orderId,
                                 @NotNull String orderNo,
                                 @Nullable String externalId,
                                 @NotNull PaymentChannel channel,
                                 @NotNull PaymentStatus status,
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
