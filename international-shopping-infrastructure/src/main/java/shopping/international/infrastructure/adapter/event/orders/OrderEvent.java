package shopping.international.infrastructure.adapter.event.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import shopping.international.domain.adapter.event.orders.IOrderEvent;
import shopping.international.domain.model.vo.orders.OrderTimeoutMessage;
import shopping.international.types.config.OrderTimeoutSettings;

/**
 * 订单消息发布 (RabbitMQ)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEvent implements IOrderEvent {

    /**
     * 消息投递 template
     */
    private final RabbitTemplate rabbitTemplate;
    /**
     * @see OrderTimeoutSettings
     */
    private final OrderTimeoutSettings orderTimeoutSettings;

    /**
     * 发布订单超时取消的延迟消息
     *
     * @param message     消息体
     * @param delayMillis 延迟毫秒数
     */
    @Override
    public void publishOrderTimeout(@NotNull OrderTimeoutMessage message, long delayMillis) {
        long safeDelay = Math.min(delayMillis, Integer.MAX_VALUE);
        CorrelationData correlation = new CorrelationData(message.orderNo());
        rabbitTemplate.convertAndSend(
                orderTimeoutSettings.exchange(),
                orderTimeoutSettings.routingKey(),
                message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    msg.getMessageProperties().setMessageId(message.orderNo());
                    msg.getMessageProperties().setHeader("x-delay", (int) safeDelay);
                    return msg;
                },
                correlation
        );
        log.info("发送订单超时取消延迟消息成功, orderNo={}, delay={} ms", message.orderNo(), safeDelay);
    }
}
