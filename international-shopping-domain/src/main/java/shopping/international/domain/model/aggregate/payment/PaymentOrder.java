package shopping.international.domain.model.aggregate.payment;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 支付单聚合根
 *
 * <p>聚合职责: </p>
 * <ul>
 *     <li>维护支付单状态机 {@code (INIT/PENDING/SUCCESS/FAIL/CLOSED/EXCEPTION)}</li>
 *     <li>承载与支付网关交互的关键标识 {@code externalId}</li>
 *     <li>承载请求/响应/回调报文快照 (JSON)</li>
 * </ul>
 */
@Getter
@ToString
@Accessors(chain = true)
public class PaymentOrder implements Verifiable {

    /**
     * 主键 ID
     */
    private final Long id;

    /**
     * 订单 ID (orders.id)
     */
    private final Long orderId;

    /**
     * 支付网关 externalId (可为空)
     */
    @Nullable
    private String externalId;

    /**
     * 支付通道
     */
    private PaymentChannel channel;

    /**
     * 支付金额 (最小货币单位)
     */
    private final long amount;

    /**
     * 币种
     */
    private final String currency;

    /**
     * 支付单状态
     */
    private PaymentStatus status;

    /**
     * 下单请求报文 (JSON, 可选)
     */
    @Nullable
    private final String requestPayload;

    /**
     * 下单响应报文 (JSON, 可选)
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
     * 构造支付单聚合根
     *
     * @param id              主键 ID
     * @param orderId         订单 ID (orders.id)
     * @param externalId      支付网关 externalId (可为空)
     * @param channel         支付通道
     * @param amount          支付金额 (最小货币单位)
     * @param currency        币种
     * @param status          支付单状态
     * @param requestPayload  下单请求报文 (JSON, 可选)
     * @param responsePayload 下单响应报文 (JSON, 可选)
     * @param notifyPayload   最近一次回调报文 (JSON, 可选)
     * @param lastPolledAt    最近轮询时间
     * @param lastNotifiedAt  最近回调处理时间
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     */
    public PaymentOrder(@NotNull Long id,
                        @NotNull Long orderId,
                        @Nullable String externalId,
                        @NotNull PaymentChannel channel,
                        long amount,
                        @NotNull String currency,
                        @NotNull PaymentStatus status,
                        @Nullable String requestPayload,
                        @Nullable String responsePayload,
                        @Nullable String notifyPayload,
                        @Nullable LocalDateTime lastPolledAt,
                        @Nullable LocalDateTime lastNotifiedAt,
                        @Nullable LocalDateTime createdAt,
                        @Nullable LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.externalId = externalId;
        this.channel = channel;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.notifyPayload = notifyPayload;
        this.lastPolledAt = lastPolledAt;
        this.lastNotifiedAt = lastNotifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 判断当前支付单是否为终态
     *
     * @return 若为终态返回 true
     */
    public boolean isFinal() {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.FAIL
                || status == PaymentStatus.CLOSED
                || status == PaymentStatus.EXCEPTION;
    }

    /**
     * 将占位支付单绑定为指定通道并进入 INIT
     *
     * <p>典型场景: 订单创建时生成占位支付单 (NONE/NONE), 在用户点击“去支付”后升级为真实支付单。</p>
     *
     * @param channel 目标通道
     */
    public void activateForChannel(@NotNull PaymentChannel channel) {
        requireNotNull(channel, "channel 不能为空");
        require(this.status == PaymentStatus.NONE, "仅 NONE 状态允许激活支付单");
        this.channel = channel;
        this.status = PaymentStatus.INIT;
    }

    /**
     * 回填支付网关 externalId 并推进为 PENDING
     *
     * @param externalId 网关 externalId
     */
    public void markGatewayOrderCreated(@NotNull String externalId) {
        requireNotNull(externalId, "externalId 不能为空");
        require(this.status == PaymentStatus.INIT || this.status == PaymentStatus.PENDING, "仅 INIT/PENDING 状态允许回填 externalId");
        this.externalId = externalId.strip();
        this.status = PaymentStatus.PENDING;
    }

    /**
     * 标记支付成功
     */
    public void markSuccess() {
        require(this.status == PaymentStatus.INIT || this.status == PaymentStatus.PENDING, "仅 INIT/PENDING 状态允许标记 SUCCESS");
        this.status = PaymentStatus.SUCCESS;
    }

    /**
     * 标记支付失败
     */
    public void markFail() {
        require(this.status == PaymentStatus.INIT || this.status == PaymentStatus.PENDING, "仅 INIT/PENDING 状态允许标记 FAIL");
        this.status = PaymentStatus.FAIL;
    }

    /**
     * 标记支付异常
     */
    public void markException() {
        require(this.status != PaymentStatus.SUCCESS, "SUCCESS 状态不允许标记为 EXCEPTION");
        this.status = PaymentStatus.EXCEPTION;
    }

    /**
     * 关闭支付单 (取消本次支付尝试)
     */
    public void close() {
        if (this.status == PaymentStatus.CLOSED)
            return;
        require(this.status == PaymentStatus.INIT || this.status == PaymentStatus.PENDING || this.status == PaymentStatus.NONE, "仅 NONE/INIT/PENDING 状态允许关闭");
        this.status = PaymentStatus.CLOSED;
    }

    /**
     * 支付单不变式校验
     */
    @Override
    public void validate() {
        requireNotNull(id, "payment.id 不能为空");
        requireNotNull(orderId, "payment.orderId 不能为空");
        requireNotNull(channel, "payment.channel 不能为空");
        require(amount > 0, "payment.amount 必须大于 0");
        requireNotNull(currency, "payment.currency 不能为空");
        requireNotNull(status, "payment.status 不能为空");
    }
}

