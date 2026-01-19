package shopping.international.types.config;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * 订单超时取消消息的配置快照
 *
 * @param ttl                订单超时时间
 * @param cancelReason       自动取消订单登记的取消原因
 * @param exchange           交换机名称
 * @param queue              队列名称
 * @param routingKey         路由 key 名称
 * @param recoveryBatchSize  订单超时扫描批次大小
 */
@Builder
public record OrderTimeoutSettings(@NotNull Duration ttl,
                                   @NotNull String cancelReason,
                                   @NotNull String exchange,
                                   @NotNull String queue,
                                   @NotNull String routingKey,
                                   @NotNull Integer recoveryBatchSize) {
}
