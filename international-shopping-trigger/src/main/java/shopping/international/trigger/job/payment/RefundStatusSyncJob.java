package shopping.international.trigger.job.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shopping.international.domain.service.orders.IAdminOrderService;

/**
 * 退款状态兜底同步任务 (低频)
 *
 * <p>用于兜底处理以下场景:</p>
 * <ul>
 *     <li>退款返回 PENDING, Webhook 未到达或未成功处理</li>
 *     <li>Webhook 已更新 payment_refund 为 SUCCESS, 但订单仍处于 REFUNDING, 需要补推进与回补库存</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundStatusSyncJob {

    /**
     * 管理侧订单服务
     */
    private final IAdminOrderService orderService;

    /**
     * 单批扫描的最大退款单数量 (默认 50)
     */
    @Value("${refunds.sync.batch-size:50}")
    private int batchSize;

    /**
     * 低频兜底同步任务
     *
     * <p>默认固定延迟 30 分钟, 可通过配置 {@code refunds.sync.fixed-delay} 覆盖</p>
     */
    @Scheduled(fixedDelayString = "${refunds.sync.fixed-delay}")
    public void sync() {
        try {
            int processed = orderService.syncNonFinalRefunds(batchSize);
            if (processed > 0)
                log.info("退款状态兜底同步完成, processed: {}", processed);
        } catch (Exception e) {
            log.error("退款状态兜底同步任务执行失败: {}", e.getMessage(), e);
        }
    }
}

