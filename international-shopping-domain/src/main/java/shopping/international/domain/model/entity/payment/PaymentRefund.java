package shopping.international.domain.model.entity.payment;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.RefundInitiator;
import shopping.international.domain.model.enums.payment.RefundReasonCode;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.payment.RefundNo;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 退款单实体 (对应表 payment_refund)
 *
 * <p>说明: 退款创建入口收口在 Orders 域, 本实体用于 Payment 域的状态追踪与对账</p>
 */
@Getter
@ToString
public class PaymentRefund implements Verifiable {

    /**
     * 主键 ID
     */
    private final Long id;

    /**
     * 退款单号 (对外可见)
     */
    private final RefundNo refundNo;

    /**
     * 订单 ID (orders.id)
     */
    private final Long orderId;

    /**
     * 原支付单 ID (payment_order.id)
     */
    private final Long paymentOrderId;

    /**
     * 网关退款单号 (可为空)
     */
    @Nullable
    private final String externalRefundId;

    /**
     * 商户侧/客户端幂等键 (可选)
     */
    @Nullable
    private final String clientRefundNo;

    /**
     * 退款总金额 (最小货币单位)
     */
    private final long amount;

    /**
     * 币种
     */
    private final String currency;

    /**
     * 货品部分退款金额 (可选)
     */
    @Nullable
    private final Long itemsAmount;

    /**
     * 运费部分退款金额 (可选)
     */
    @Nullable
    private final Long shippingAmount;

    /**
     * 退款状态
     */
    private RefundStatus status;

    /**
     * 原因分类
     */
    private final RefundReasonCode reasonCode;

    /**
     * 原因备注 (可选)
     */
    @Nullable
    private final String reasonText;

    /**
     * 发起方
     */
    private final RefundInitiator initiator;

    /**
     * 关联工单 ID (可选)
     */
    @Nullable
    private final Long ticketId;

    /**
     * 退款请求报文 (JSON, 可选)
     */
    @Nullable
    private final String requestPayload;

    /**
     * 退款响应报文 (JSON, 可选)
     */
    @Nullable
    private final String responsePayload;

    /**
     * 最近一次回调报文 (JSON, 可选)
     */
    @Nullable
    private final String notifyPayload;

    /**
     * 最近轮询时间
     */
    @Nullable
    private final LocalDateTime lastPolledAt;

    /**
     * 最近回调处理时间
     */
    @Nullable
    private final LocalDateTime lastNotifiedAt;

    /**
     * 创建时间
     */
    @Nullable
    private final LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Nullable
    private final LocalDateTime updatedAt;

    /**
     * 退款明细列表 (可选)
     */
    private final List<PaymentRefundItem> items;

    /**
     * 构造退款单实体
     *
     * @param id               主键 ID
     * @param refundNo         退款单号 (对外可见)
     * @param orderId          订单 ID (orders.id)
     * @param paymentOrderId   原支付单 ID (payment_order.id)
     * @param externalRefundId 网关退款单号 (可为空)
     * @param clientRefundNo   商户侧/客户端幂等键 (可选)
     * @param amount           退款总金额 (最小货币单位)
     * @param currency         币种
     * @param itemsAmount      货品部分退款金额 (可选)
     * @param shippingAmount   运费部分退款金额 (可选)
     * @param status           退款状态
     * @param reasonCode       原因分类
     * @param reasonText       原因备注 (可选)
     * @param initiator        发起方
     * @param ticketId         关联工单 ID (可选)
     * @param requestPayload   退款请求报文 (JSON, 可选)
     * @param responsePayload  退款响应报文 (JSON, 可选)
     * @param notifyPayload    最近一次回调报文 (JSON, 可选)
     * @param lastPolledAt     最近轮询时间
     * @param lastNotifiedAt   最近回调处理时间
     * @param createdAt        创建时间
     * @param updatedAt        更新时间
     * @param items            退款明细列表 (可选)
     */
    public PaymentRefund(@NotNull Long id,
                         @NotNull RefundNo refundNo,
                         @NotNull Long orderId,
                         @NotNull Long paymentOrderId,
                         @Nullable String externalRefundId,
                         @Nullable String clientRefundNo,
                         long amount,
                         @NotNull String currency,
                         @Nullable Long itemsAmount,
                         @Nullable Long shippingAmount,
                         @NotNull RefundStatus status,
                         @NotNull RefundReasonCode reasonCode,
                         @Nullable String reasonText,
                         @NotNull RefundInitiator initiator,
                         @Nullable Long ticketId,
                         @Nullable String requestPayload,
                         @Nullable String responsePayload,
                         @Nullable String notifyPayload,
                         @Nullable LocalDateTime lastPolledAt,
                         @Nullable LocalDateTime lastNotifiedAt,
                         @Nullable LocalDateTime createdAt,
                         @Nullable LocalDateTime updatedAt,
                         @NotNull List<PaymentRefundItem> items) {
        this.id = id;
        this.refundNo = refundNo;
        this.orderId = orderId;
        this.paymentOrderId = paymentOrderId;
        this.externalRefundId = externalRefundId;
        this.clientRefundNo = clientRefundNo;
        this.amount = amount;
        this.currency = currency;
        this.itemsAmount = itemsAmount;
        this.shippingAmount = shippingAmount;
        this.status = status;
        this.reasonCode = reasonCode;
        this.reasonText = reasonText;
        this.initiator = initiator;
        this.ticketId = ticketId;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.notifyPayload = notifyPayload;
        this.lastPolledAt = lastPolledAt;
        this.lastNotifiedAt = lastNotifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.items = List.copyOf(items);
    }

    /**
     * 标记退款单为处理中
     */
    public void markPending() {
        require(status == RefundStatus.INIT, "仅 INIT 状态允许进入 PENDING");
        status = RefundStatus.PENDING;
    }

    /**
     * 标记退款成功
     */
    public void markSuccess() {
        require(status == RefundStatus.INIT || status == RefundStatus.PENDING, "仅 INIT/PENDING 状态允许进入 SUCCESS");
        status = RefundStatus.SUCCESS;
    }

    /**
     * 标记退款失败
     */
    public void markFail() {
        require(status == RefundStatus.INIT || status == RefundStatus.PENDING, "仅 INIT/PENDING 状态允许进入 FAIL");
        status = RefundStatus.FAIL;
    }

    /**
     * 校验退款单不变式
     */
    @Override
    public void validate() {
        requireNotNull(id, "refund.id 不能为空");
        requireNotNull(refundNo, "refund.refundNo 不能为空");
        refundNo.validate();
        requireNotNull(orderId, "refund.orderId 不能为空");
        requireNotNull(paymentOrderId, "refund.paymentOrderId 不能为空");
        require(amount > 0, "refund.amount 必须大于 0");
        requireNotNull(currency, "refund.currency 不能为空");
        requireNotNull(status, "refund.status 不能为空");
        requireNotNull(reasonCode, "refund.reasonCode 不能为空");
        requireNotNull(initiator, "refund.initiator 不能为空");
    }
}

