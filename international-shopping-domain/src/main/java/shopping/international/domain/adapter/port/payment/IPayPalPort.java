package shopping.international.domain.adapter.port.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * PayPal 支付网关端口 (Port)
 *
 * <ul>
 *     <li>对接 PayPal 的 OAuth2 Token, Orders, Capture, Refund, Webhook 验签等 API</li>
 *     <li>屏蔽第三方网络调用与鉴权细节, 为领域服务提供稳定抽象</li>
 * </ul>
 */
public interface IPayPalPort {

    /**
     * 创建 PayPal Order (用于生成收银台跳转链接)
     *
     * @param cmd 创建订单命令
     * @return 创建结果
     */
    @NotNull CreateOrderResult createOrder(@NotNull CreateOrderCommand cmd);

    /**
     * 查询 PayPal Order (用于复用已有 externalId 获取 approve link 或状态)
     *
     * @param paypalOrderId PayPal Order ID
     * @return 查询结果
     */
    @NotNull GetOrderResult getOrder(@NotNull String paypalOrderId);

    /**
     * Capture PayPal Order (确认扣款)
     *
     * @param cmd Capture 命令
     * @return Capture 结果
     */
    @NotNull CaptureOrderResult captureOrder(@NotNull CaptureOrderCommand cmd);

    /**
     * Refund PayPal Capture (按 capture_id 退款)
     *
     * @param cmd Refund 命令
     * @return Refund 结果
     */
    @NotNull RefundCaptureResult refundCapture(@NotNull RefundCaptureCommand cmd);

    /**
     * 校验 PayPal Webhook 请求来源 (验签 + 防重放)
     *
     * <p>建议在通过该方法校验后, 才进入领域逻辑更新本地支付单与订单冗余字段</p>
     *
     * @param cmd 验签命令 (包含签名相关 Header 与 webhook_event body)
     */
    void verifyWebhookAndReplayProtection(@NotNull VerifyWebhookCommand cmd);

    /**
     * 创建 PayPal Order 命令
     *
     * @param idempotencyKey 幂等键 (将透传到 PayPal-Request-Id)
     * @param returnUrl      支付成功回跳地址
     * @param cancelUrl      用户取消回跳地址
     * @param amountMinor    金额 (最小货币单位)
     * @param currency       币种 (如 USD)
     */
    record CreateOrderCommand(@NotNull String idempotencyKey,
                              @NotNull String returnUrl,
                              @NotNull String cancelUrl,
                              long amountMinor,
                              @NotNull String currency) {
    }

    /**
     * 创建 PayPal Order 结果
     *
     * @param paypalOrderId PayPal Order ID
     * @param approveUrl    收银台跳转链接 (rel=approve)
     * @param requestJson   原始请求 JSON (用于落库快照)
     * @param responseJson  原始响应 JSON (用于落库快照)
     */
    record CreateOrderResult(@NotNull String paypalOrderId,
                             @NotNull String approveUrl,
                             @NotNull String requestJson,
                             @NotNull String responseJson) {
    }

    /**
     * 查询 PayPal Order 结果
     *
     * @param paypalOrderId PayPal Order ID
     * @param status        PayPal 侧状态 (原样字符串)
     * @param approveUrl    收银台跳转链接 (可能为空)
     * @param captureId     若已 capture, 返回 capture_id (可能为空)
     * @param captureTime   若已 capture, 返回 capture_time (可能为空)
     * @param responseJson  原始响应 JSON (用于落库快照/排障)
     */
    record GetOrderResult(@NotNull String paypalOrderId,
                          @NotNull String status,
                          @Nullable String approveUrl,
                          @Nullable String captureId,
                          @Nullable OffsetDateTime captureTime,
                          @NotNull String responseJson) {
    }

    /**
     * Capture 命令
     *
     * @param idempotencyKey 幂等键 (可选, 将透传到 PayPal-Request-Id)
     * @param paypalOrderId  PayPal Order ID
     * @param payerId        可选 payer_id
     * @param note           可选备注
     */
    record CaptureOrderCommand(@Nullable String idempotencyKey,
                               @NotNull String paypalOrderId,
                               @Nullable String payerId,
                               @Nullable String note) {
    }

    /**
     * Capture 结果
     *
     * @param paypalOrderId PayPal Order ID
     * @param captureId     capture_id (可能为空, 取决于响应)
     * @param captureTime   capture_time (可能为空)
     * @param status        capture 结果状态 (原样字符串)
     * @param requestJson   原始请求 JSON (用于落库快照)
     * @param responseJson  原始响应 JSON (用于落库快照)
     */
    record CaptureOrderResult(@NotNull String paypalOrderId,
                              @Nullable String captureId,
                              @Nullable OffsetDateTime captureTime,
                              @NotNull String status,
                              @NotNull String requestJson,
                              @NotNull String responseJson) {
    }

    /**
     * Refund 命令
     *
     * @param idempotencyKey 幂等键 (可选, 将透传到 PayPal-Request-Id)
     * @param captureId      capture_id
     * @param amountMinor    退款金额 (最小货币单位)
     * @param currency       币种
     * @param note           可选备注
     */
    record RefundCaptureCommand(@Nullable String idempotencyKey,
                                @NotNull String captureId,
                                long amountMinor,
                                @NotNull String currency,
                                @Nullable String note) {
    }

    /**
     * Refund 结果
     *
     * @param captureId    capture_id
     * @param refundId     PayPal Refund ID (可能为空)
     * @param status       退款结果状态 (原样字符串)
     * @param requestJson  原始请求 JSON (用于落库快照)
     * @param responseJson 原始响应 JSON (用于落库快照)
     */
    record RefundCaptureResult(@NotNull String captureId,
                               @Nullable String refundId,
                               @NotNull String status,
                               @NotNull String requestJson,
                               @NotNull String responseJson) {
    }

    /**
     * Webhook 验签命令
     *
     * @param authAlgo          PAYPAL-AUTH-ALGO
     * @param certUrl           PAYPAL-CERT-URL
     * @param transmissionId    PAYPAL-TRANSMISSION-ID
     * @param transmissionSig   PAYPAL-TRANSMISSION-SIG
     * @param transmissionTime  PAYPAL-TRANSMISSION-TIME
     * @param webhookEvent      webhook_event 原始对象 (建议为 Map 结构)
     * @param eventIdForDedupe  事件 ID (用于去重, 通常为 webhook_event.id)
     * @param replayTtl         防重放 TTL
     */
    record VerifyWebhookCommand(@NotNull String authAlgo,
                                @NotNull String certUrl,
                                @NotNull String transmissionId,
                                @NotNull String transmissionSig,
                                @NotNull String transmissionTime,
                                @NotNull Map<String, Object> webhookEvent,
                                @NotNull String eventIdForDedupe,
                                @NotNull Duration replayTtl) {
    }

    /**
     * 从 PayPal Webhook event 中尽可能提取 PayPal Order ID (external_id)
     *
     * <p>不同 event_type 的资源结构不一致, 因此返回 Optional</p>
     *
     * @param webhookEvent webhook_event
     * @return PayPal Order ID (若可提取)
     */
    @NotNull Optional<String> tryExtractPayPalOrderId(@NotNull Map<String, Object> webhookEvent);
}
