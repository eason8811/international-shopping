package shopping.international.trigger.job.shipping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shopping.international.domain.service.shipping.IAdminShipmentService;

/**
 * 物流占位单补偿任务, 定期扫描 PAID 且无 shipment 的订单并补建占位物流单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentPlaceholderCompensateJob {

    /**
     * 管理侧物流领域服务
     */
    private final IAdminShipmentService adminShipmentService;

    /**
     * 单次补偿扫描数量
     */
    @Value("${shipping.placeholder-compensate.batch-size:100}")
    private int batchSize;

    /**
     * 定时执行补偿任务
     */
    @Scheduled(fixedDelayString = "${shipping.placeholder-compensate.fixed-delay:2h}")
    public void compensate() {
        try {
            int compensated = adminShipmentService.compensatePaidOrdersWithoutShipment(
                    batchSize,
                    "shipping:placeholder:compensate"
            );
            if (compensated > 0)
                log.info("占位物流单补偿完成, compensated={}", compensated);
        } catch (Exception exception) {
            log.error("占位物流单补偿任务执行失败, err={}", exception.getMessage(), exception);
        }
    }
}
