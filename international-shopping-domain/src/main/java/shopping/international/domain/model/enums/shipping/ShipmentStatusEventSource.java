package shopping.international.domain.model.enums.shipping;

/**
 * 物流状态事件来源枚举
 *
 * <p>用于标识状态日志的来源渠道, 与 {@code shipment_status_log.source_type} 保持一致</p>
 */
public enum ShipmentStatusEventSource {
    /**
     * 承运商回调
     */
    CARRIER_WEBHOOK,
    /**
     * 承运商主动轮询
     */
    CARRIER_POLL,
    /**
     * 系统任务
     */
    SYSTEM_JOB,
    /**
     * 人工操作
     */
    MANUAL,
    /**
     * 开放接口调用
     */
    API
}
