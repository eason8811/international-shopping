package shopping.international.types.exceptions;

/**
 * PayPal Webhook 防重放命中异常
 *
 * <p>用于在防重放判定为重复事件时, 通过抛出异常中断后续业务处理。</p>
 *
 * <p>该异常通常应在 Webhook 入口处被捕获并转换为幂等 OK 响应, 以避免 PayPal 重试放大。</p>
 */
public class PayPalWebhookReplayException extends RuntimeException {

    public PayPalWebhookReplayException(String message) {
        super(message);
    }
}

