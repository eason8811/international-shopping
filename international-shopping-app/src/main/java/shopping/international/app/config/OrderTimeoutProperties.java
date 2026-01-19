package shopping.international.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 订单超时取消相关配置
 */
@Data
@ConfigurationProperties(prefix = "orders.timeout")
public class OrderTimeoutProperties {
    /**
     * 超时时间窗口, 到达后触发取消 (默认 15 分钟)
     */
    private Duration ttl;
    /**
     * 取消原因文案
     */
    private String cancelReason;
    /**
     * MQ 配置
     */
    private Mq mq;
    /**
     * 兜底扫描配置
     */
    private Recovery recovery;

    @Data
    public static class Mq {
        /**
         * 延迟交换机名称
         */
        private String exchange;
        /**
         * 延迟队列名称
         */
        private String queue;
        /**
         * 路由键
         */
        private String routingKey;
        /**
         * 延迟交换机底层转发类型
         */
        private String delayedType;
        /**
         * Listener 并发消费线程数
         */
        private Integer listenerConcurrency;
        /**
         * Listener 预取条数
         */
        private Integer listenerPrefetch;
    }

    @Data
    public static class Recovery {
        /**
         * 定时兜底扫描的固定延迟
         */
        private Duration fixedDelay;
        /**
         * 单批扫描的最大订单数量
         */
        private Integer batchSize;
    }
}
