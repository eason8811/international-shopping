package shopping.international.domain.adapter.repository.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.domain.model.vo.payment.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付领域仓储接口 (Repository)
 *
 * <ul>
 *     <li>支付单的创建/复用/状态变更 (含并发安全 CAS)</li>
 *     <li>在基础设施层实现跨领域数据一致: payment_order 与 orders 的冗余字段同步</li>
 * </ul>
 */
public interface IPaymentRepository {

    /**
     * 在同库事务内准备 PayPal Checkout (锁定订单行, 关闭旧待支付支付单, 创建/复用 PAYPAL 支付单, 同步 orders 冗余字段)
     *
     * @param userId  用户 ID
     * @param orderNo 订单号 (orders.order_no)
     * @return 事务内准备结果
     */
    @NotNull PayPalCheckoutResult preparePayPalCheckout(@NotNull Long userId, @NotNull String orderNo);

    /**
     * 回填外部订单 ID, 并将支付单推进为 PENDING, 同时同步 orders.payment_external_id / orders.pay_status
     *
     * @param paymentId       支付单 ID
     * @param externalId      外部订单 ID
     * @param requestPayload  下单请求报文 (JSON 字符串)
     * @param responsePayload 下单响应报文 (JSON 字符串)
     */
    void bindPayPalOrder(@NotNull Long paymentId,
                         @NotNull String externalId,
                         @Nullable String requestPayload,
                         @Nullable String responsePayload);

    /**
     * 用户侧取消本次支付尝试: payment_order.status INIT/PENDING -> CLOSED (CAS), 并同步 orders.pay_status -> CLOSED
     *
     * @param userId    用户 ID
     * @param paymentId 支付单 ID
     * @return 操作结果
     */
    @NotNull PaymentResultView cancelPayPalPayment(@NotNull Long userId, @NotNull Long paymentId);

    /**
     * 获取用于 capture 的目标信息 (不加锁)
     *
     * @param userId    用户 ID
     * @param paymentId 支付单 ID
     * @return capture 目标
     */
    @NotNull CaptureTarget getCaptureTarget(@NotNull Long userId, @NotNull Long paymentId);

    /**
     * 获取用于同步/回调处理的目标信息 (不做用户校验)
     *
     * @param paymentId 支付单 ID
     * @return capture 目标
     */
    @NotNull CaptureTarget getCaptureTargetForOps(@NotNull Long paymentId);

    /**
     * 在同库事务内应用 PayPal capture 结果 (更新 payment_order 与同步 orders 冗余字段)
     *
     * <p>该方法应承载幂等与并发安全的 "权威落库逻辑"：
     * 在基础设施层持锁读取 orders 当前状态后统一判定 SUCCESS/EXCEPTION/FAIL 与是否推进订单为 PAID。</p>
     */
    @NotNull PaymentResultView applyPayPalCaptureResult(@NotNull PayPalCaptureApplyCommand cmd);


    /**
     * 按 PayPal Order ID 查找本地支付单 ID (payment_order.external_id 唯一)
     *
     * @param paypalOrderId PayPal Order ID
     * @return 支付单 ID (可为空)
     */
    @NotNull Optional<Long> findPaymentIdByPayPalOrderId(@NotNull String paypalOrderId);

    /**
     * 扫描需要同步的支付单 (用于低频兜底任务)
     *
     * @param limit 最大数量
     * @return 候选列表
     */
    @NotNull List<SyncCandidate> listSyncCandidates(int limit);

    /**
     * 记录轮询时间与轮询报文 (可为空)
     *
     * @param paymentId       支付单 ID
     * @param polledAt        轮询时间
     * @param responsePayload 轮询响应报文 (JSON, 可为空)
     * @param captureId       PayPal capture_id (可为空, 便于后续查单/退款)
     */
    void markPolled(@NotNull Long paymentId,
                    @NotNull LocalDateTime polledAt,
                    @Nullable String responsePayload,
                    @Nullable String captureId);

    /**
     * 运维/兜底: 关闭 PayPal 支付尝试 (不做用户校验)
     *
     * <p>用于 PayPal 侧 order.status=VOIDED 等场景: 将当前支付单推进为 CLOSED (CAS), 并在其为当前有效尝试时同步 orders.pay_status -> CLOSED</p>
     *
     * @param paymentId 支付单 ID
     */
    void closePayPalPaymentForOps(@NotNull Long paymentId);

    /**
     * 写入自动退款记录 (用于晚到支付等自动退款补偿)
     *
     * <p>该方法为 Payment 域的事实表落库, 不负责订单域库存处理</p>
     *
     * @param orderId          订单 ID
     * @param paymentOrderId   支付单 ID
     * @param refundNo         退款单号 (业务侧生成)
     * @param externalRefundId 网关退款单号 (可空)
     * @param clientRefundNo   商户侧/客户端幂等键 (可空, 用于去重)
     * @param amountMinor      退款金额 (最小货币单位)
     * @param currency         币种
     * @param status           退款状态
     * @param requestPayload   退款请求报文 (JSON, 可空)
     * @param responsePayload  退款响应报文 (JSON, 可空)
     * @return 退款单主键 ID
     */
    @NotNull Long insertRefund(@NotNull Long orderId,
                               @NotNull Long paymentOrderId,
                               @NotNull String refundNo,
                               @Nullable String externalRefundId,
                               @Nullable String clientRefundNo,
                               long amountMinor,
                               @NotNull String currency,
                               @NotNull RefundStatus status,
                               @Nullable String requestPayload,
                               @Nullable String responsePayload);

    /**
     * 检查是否存在指定的退款去重键
     *
     * @param paymentOrderId 支付单 ID, 用于关联特定支付记录
     * @param clientRefundNo 商户侧或客户端提供的幂等键, 用于防止重复退款
     * @return 如果存在具有相同 <code>clientRefundNo</code> 的退款记录, 则返回 <code>true</code>; 否则返回 <code>false</code>
     */
    boolean existsRefundDedupeKey(@NotNull Long paymentOrderId, @NotNull String clientRefundNo);

}
