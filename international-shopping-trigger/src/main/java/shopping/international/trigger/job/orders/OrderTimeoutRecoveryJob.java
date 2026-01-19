package shopping.international.trigger.job.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.types.config.OrderTimeoutSettings;
import shopping.international.types.exceptions.ConflictException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单超时取消兜底任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutRecoveryJob {

    /**
     * 管理侧订单服务
     */
    private final IAdminOrderService orderService;
    /**
     * @see OrderTimeoutSettings
     */
    private final OrderTimeoutSettings orderTimeoutSettings;

    /**
     * 超时订单取消兜底定时任务
     */
    @Scheduled(fixedDelayString = "${orders.timeout.recovery.fixed-delay}")
    public void recover() {
        try {
            int batchSize = orderTimeoutSettings.recoveryBatchSize();
            LocalDateTime deadline = LocalDateTime.now().minus(orderTimeoutSettings.ttl());
            var candidates = orderService.listTimeoutCandidates(deadline, batchSize);
            if (candidates.isEmpty())
                return;
            log.info("订单超时兜底扫描, 批次大小: {}, DDL: {}", candidates.size(), deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.xxx")));
            for (var order : candidates) {
                try {
                    OrderNo orderNo = order.getOrderNo();
                    orderService.cancelUnpaid(orderNo, orderTimeoutSettings.cancelReason(), OrderStatusEventSource.SCHEDULER);
                } catch (ConflictException e) {
                    log.debug("兜底取消时订单状态已变更, orderNo={}, msg={}", order.getOrderNo().getValue(), e.getMessage());
                }
            }
            log.info("订单超时兜底扫描完成");
        } catch (Exception e) {
            log.error("订单超时兜底任务执行失败: {}", e.getMessage(), e);
        }
    }
}
