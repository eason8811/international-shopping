package shopping.international.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import shopping.international.trigger.controller.customerservice.TicketWsController;

import static shopping.international.types.constant.SecurityConstants.API_PREFIX;

/**
 * 售后服务 WebSocket 配置, 注册 customerservice 实时消息连接入口
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CustomerServiceWebSocketConfig implements WebSocketConfigurer {

    /**
     * 售后 WebSocket 控制器
     */
    private final TicketWsController ticketWsController;

    /**
     * 注册 WebSocket 处理器
     *
     * @param registry WebSocket 处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ticketWsController, API_PREFIX + "/ws/customerservice")
                .setAllowedOriginPatterns("*");
    }
}
