package shopping.international.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import shopping.international.types.config.OrderTimeoutSettings;

import java.util.Map;

/**
 * 订单超时取消的 RabbitMQ 装配
 */
@Slf4j
@Configuration
@EnableRabbit
@RequiredArgsConstructor
@EnableConfigurationProperties(OrderTimeoutProperties.class)
public class OrderTimeoutRabbitConfig {

    /**
     * 订单超时取消相关配置
     */
    private final OrderTimeoutProperties properties;

    /**
     * 依据配置文件中的订单超时设置信息, 注入一个 {@link OrderTimeoutSettings} Bean
     *
     * @return {@link OrderTimeoutSettings} Bean
     */
    @Bean
    public OrderTimeoutSettings orderTimeoutSettings() {
        return OrderTimeoutSettings.builder()
                .ttl(properties.getTtl())
                .cancelReason(properties.getCancelReason())
                .exchange(properties.getMq().getExchange())
                .queue(properties.getMq().getQueue())
                .routingKey(properties.getMq().getRoutingKey())
                .recoveryBatchSize(properties.getRecovery().getBatchSize())
                .build();
    }

    /**
     * 注入 JSON 消息转换器 {@link Jackson2JsonMessageConverter} Bean
     *
     * @return {@link Jackson2JsonMessageConverter} Bean, 实现 JSON 消息内容的序列化和反序列化
     */
    @Bean
    public MessageConverter orderMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 注入 {@link RabbitTemplate} Bean, 用于发送和接收消息
     *
     * @param connectionFactory     提供连接到 RabbitMQ 服务器的工厂
     * @param orderMessageConverter 配置的 JSON 消息转换器
     * @return {@link RabbitTemplate} Bean, 已配置可靠投递和可靠消费
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter orderMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(orderMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack)
                log.warn("订单超时消息发布失败, correlationId={}, cause={}",
                        correlationData == null ? null : correlationData.getId(), cause);
        });
        template.setReturnsCallback(returned ->
                log.warn("订单超时消息路由失败, exchange={}, routingKey={}, replyCode={}, replyText={}, messageId={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText(),
                        returned.getMessage().getMessageProperties().getMessageId())
        );
        return template;
    }

    /**
     * 注入一个 {@link CustomExchange} 自定义交换机 Bean,
     *
     * @return {@link CustomExchange} 对象, 配置了延迟消息类型和是否持久化等属性
     */
    @Bean
    public CustomExchange orderTimeoutExchange() {
        Map<String, Object> args = Map.of("x-delayed-type", properties.getMq().getDelayedType());
        return new CustomExchange(properties.getMq().getExchange(), "x-delayed-message", true, false, args);
    }

    /**
     * 注入一个指定名称的 {@link Queue} 持久化队列 Bean
     *
     * @return {@link Queue} 持久化队列 Bean
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue(properties.getMq().getQueue(), true);
    }

    /**
     * 创建一个绑定, 将指定的队列与自定义交换机通过配置文件中定义的路由键关联起来
     *
     * @param orderTimeoutExchange 自定义交换机, 用于处理延迟消息
     * @param orderTimeoutQueue    持久化队列, 接收来自自定义交换机的消息
     * @return 返回一个 {@link Binding} 对象, 表示队列和交换机之间的绑定关系
     */
    @Bean
    public Binding orderTimeoutBinding(CustomExchange orderTimeoutExchange, Queue orderTimeoutQueue) {
        return BindingBuilder.bind(orderTimeoutQueue)
                .to(orderTimeoutExchange)
                .with(properties.getMq().getRoutingKey())
                .noargs();
    }

    /**
     * 注入一个 {@link SimpleRabbitListenerContainerFactory} Bean, 用于处理订单相关的消息监听器容器
     *
     * @param connectionFactory     提供连接到 RabbitMQ 服务器的工厂
     * @param orderMessageConverter 配置的消息转换器, 用于消息内容的序列化和反序列化
     * @return 返回已配置好的 {@link SimpleRabbitListenerContainerFactory} 实例, 包含了连接工厂, 消息转换器, 手动确认模式等设置, 根据配置文件中的相关属性还可能设置了预取计数和并发消费者数量
     */
    @Bean(name = "orderListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory orderListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              MessageConverter orderMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(orderMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        if (properties.getMq().getListenerPrefetch() != null)
            factory.setPrefetchCount(properties.getMq().getListenerPrefetch());
        if (properties.getMq().getListenerConcurrency() != null)
            factory.setConcurrentConsumers(properties.getMq().getListenerConcurrency());
        return factory;
    }
}
