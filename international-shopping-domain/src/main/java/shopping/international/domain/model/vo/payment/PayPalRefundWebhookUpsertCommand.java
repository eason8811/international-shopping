package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * PayPal 退款 Webhook Upsert 命令
 *
 * <p>用于处理 {@code PAYMENT.CAPTURE.REFUNDED / PAYMENT.CAPTURE.REVERSED} 等事件:
 * 若命中已有退款单则更新状态与 notifyPayload, 否则创建一条用于对账的退款事实记录</p>
 *
 * @param orderId          订单 ID
 * @param paymentOrderId   支付单 ID
 * @param refundNo         系统退款单号
 * @param externalRefundId 外部退款单号
 * @param clientRefundNo   客户端退款单号
 * @param amountMinor      价格 Minor 形式
 * @param currency         币种
 * @param status           状态
 * @param webhookEvent     WebHook 回调事件
 * @param notifiedAt       回调时间
 */
public record PayPalRefundWebhookUpsertCommand(@NotNull Long orderId,
                                               @NotNull Long paymentOrderId,
                                               @NotNull String refundNo,
                                               @Nullable String externalRefundId,
                                               @Nullable String clientRefundNo,
                                               long amountMinor,
                                               @NotNull String currency,
                                               @NotNull RefundStatus status,
                                               @NotNull Map<String, Object> webhookEvent,
                                               @NotNull LocalDateTime notifiedAt) implements Verifiable {
    @Override
    public void validate() {
        requireNotNull(orderId, "orderId 不能为空");
        requireNotNull(paymentOrderId, "paymentOrderId 不能为空");
        requireNotNull(refundNo, "refundNo 不能为空");
        require(refundNo.length() <= 64, "refundNo 长度不能超过 64");
        require(amountMinor > 0, "amountMinor 必须大于 0");
        requireNotNull(currency, "currency 不能为空");
        require(!currency.isBlank(), "currency 不能为空");
        requireNotNull(status, "status 不能为空");
        requireNotNull(webhookEvent, "webhookEvent 不能为空");
        requireNotNull(notifiedAt, "notifiedAt 不能为空");
    }
}

