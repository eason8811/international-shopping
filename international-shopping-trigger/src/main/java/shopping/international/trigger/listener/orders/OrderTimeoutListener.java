package shopping.international.trigger.listener.orders;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import shopping.international.domain.model.enums.orders.OrderStatusEventSource;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.orders.OrderTimeoutMessage;
import shopping.international.domain.service.orders.IAdminOrderService;
import shopping.international.types.config.OrderTimeoutSettings;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

/**
 * 订单超时取消消息监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutListener {

    /**
     * 管理侧订单服务
     */
    private final IAdminOrderService orderService;
    /**
     * @see OrderTimeoutSettings
     */
    private final OrderTimeoutSettings orderTimeoutSettings;

    /**
     * 处理订单超时消息, 根据消息内容取消未支付的订单
     *
     * @param message     订单超时消息, 包含 {@code orderNo}, {@code userId} 和 {@code createdAt}
     * @param channel     当前消息所在的通道, 用于确认或拒绝消息
     * @param deliveryTag 消息的交付标签, 用于确认或拒绝特定的消息
     * @throws Exception 如果处理过程中出现异常, 将抛出此异常
     */
    @RabbitListener(queues = "#{orderTimeoutSettings.queue()}", containerFactory = "orderListenerContainerFactory")
    public void onTimeout(OrderTimeoutMessage message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws Exception {
        try {
            OrderNo orderNo = OrderNo.of(message.orderNo());
            orderService.cancelUnpaid(orderNo, orderTimeoutSettings.cancelReason(), OrderStatusEventSource.SYSTEM);
            channel.basicAck(deliveryTag, false);
        } catch (IllegalArgumentException e) {
            log.warn("订单超时消息格式错误, payload={}, msg={}", message, e.getMessage());
            channel.basicAck(deliveryTag, false);
        } catch (ConflictException | IllegalParamException e) {
            log.info("订单超时消息跳过, orderNo={}, msg={}", message.orderNo(), e.getMessage());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理订单超时消息失败, orderNo={}, err={}", message.orderNo(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
