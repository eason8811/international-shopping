package shopping.international.trigger.job.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shopping.international.domain.service.payment.IPaymentService;

/**
 * 支付状态兜底同步任务 (低频)
 *
 * <p>用于兜底处理以下场景: </p>
 * <ul>
 *     <li>用户未触发 capture (回跳未发生/前端未调用)</li>
 *     <li>Webhook 未到达或未成功处理</li>
 * </ul>
 *
 * <p>任务会扫描非终态的支付单, 并查询 PayPal 刷新状态后, 同步 payment_order 与 orders 冗余字段</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusSyncJob {

    /**
     * 支付领域服务
     */
    private final IPaymentService paymentService;

    /**
     * 单批扫描的最大支付单数量 (默认 50)
     */
    @Value("${payments.sync.batch-size:50}")
    private int batchSize;

    /**
     * 低频兜底同步任务
     *
     * <p>默认固定延迟 30 分钟, 可通过配置 {@code payments.sync.fixed-delay} 覆盖</p>
     */
    @Scheduled(fixedDelayString = "${payments.sync.fixed-delay}")
    public void sync() {
        try {
            int processed = paymentService.syncNonFinalPayments(batchSize);
            if (processed > 0)
                log.info("支付状态兜底同步完成, processed={}", processed);
        } catch (Exception e) {
            log.error("支付状态兜底同步任务执行失败: {}", e.getMessage(), e);
        }
    }
}
