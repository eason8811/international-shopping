package shopping.international.domain.adapter.event.orders;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.orders.OrderTimeoutMessage;

/**
 * 订单事件发布端口
 */
public interface IOrderEvent {

    /**
     * 发布订单超时取消的延迟消息
     *
     * @param message     消息体
     * @param delayMillis 延迟毫秒数
     */
    void publishOrderTimeout(@NotNull OrderTimeoutMessage message, long delayMillis);
}
