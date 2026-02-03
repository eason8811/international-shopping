package shopping.international.domain.model.vo.payment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 应用 PayPal capture 结果命令
 *
 * <p>在基础设施层 "持锁" 读取 orders 当前状态后完成权威判定并落库: </p>
 * <ul>
 *     <li>是否为当前有效支付尝试 (orders.active_payment_id)</li>
 *     <li>是否晚到 (orders.created_at + ttl)</li>
 *     <li>是否允许推进订单为 PAID</li>
 * </ul>
 *
 * @param paymentId       支付单 ID
 * @param orderId         订单 ID
 * @param paypalOrderId   PayPal 订单 ID
 * @param captureSuccess  捕获是否成功的标志
 * @param captureId       PayPal capture_id (可为空, 退款/查单会用到)
 * @param captureTime     捕获操作发生的时间
 * @param ttl             捕获记录的有效期时长
 * @param responsePayload PayPal 返回的响应负载 (可为空)
 * @param notifyPayload   最近一次来自 PayPal 的通知负载 (可为空)
 * @param lastNotifiedAt  最后一次收到通知的时间 (可为空)
 * @param eventSource     订单状态变更事件来源
 * @param statusLogNote   订单状态变更备注
 */
public record PayPalCaptureApplyCommand(@NotNull Long paymentId,
                                        @NotNull Long orderId,
                                        @NotNull String paypalOrderId,
                                        boolean captureSuccess,
                                        @Nullable String captureId,
                                        @NotNull LocalDateTime captureTime,
                                        @NotNull Duration ttl,
                                        @Nullable String responsePayload,
                                        @Nullable Object notifyPayload,
                                        @Nullable LocalDateTime lastNotifiedAt,
                                        @NotNull OrderStatusEventSource eventSource,
                                        @NotNull String statusLogNote) {
}
