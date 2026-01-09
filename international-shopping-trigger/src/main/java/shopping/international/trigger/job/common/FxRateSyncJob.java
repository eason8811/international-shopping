package shopping.international.trigger.job.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shopping.international.domain.service.common.IFxRateService;
import shopping.international.domain.service.orders.IAdminDiscountService;
import shopping.international.domain.service.products.ISkuService;
import shopping.international.types.config.FxRateProperties;

/**
 * FX 最新汇率同步定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateSyncJob {
    /**
     * SKU 服务
     */
    private final ISkuService skuService;
    /**
     * 折扣服务
     */
    private final IAdminDiscountService discountService;
    /**
     * 外汇汇率服务
     */
    private final IFxRateService fxRateService;
    /**
     * 外汇汇率同步设置项
     */
    private final FxRateProperties fxRateProperties;

    /**
     * 默认每小时同步一次 (需保证 fx_as_of 在 8 小时有效窗口内)
     */
    @Scheduled(
            initialDelayString = "${fx.rate.sync.initialDelay}",
            fixedDelayString = "${fx.rate.sync.fixedDelay}"
    )
    public void sync() {
        try {
            log.info("外汇汇率同步开始...");
            fxRateService.syncLatest(fxRateProperties.getBaseCurrency());
            log.info("外汇汇率同步完成");
            log.info("开始同步 SKU 价格...");
            skuService.recomputeFxPricesAll(50);
            log.info("SKU 价格同步完成");
            log.info("开始同步折扣策略 Amounts...");
            discountService.recomputeFxAmountsAll(50);
            log.info("折扣策略 Amounts 同步完成");
        } catch (Exception e) {
            log.warn("FX 同步失败: {}", e.getMessage());
        }
    }
}
