package shopping.international.types.exceptions;

/**
 * 资源未找到异常
 *
 * <p>用于表示请求的业务资源不存在 (如订单不存在、支付单不存在等), 通常应映射为 HTTP 404</p>
 */
public class NotFoundException extends RuntimeException {

    /**
     * 构造一个资源未找到异常
     *
     * @param message 错误信息
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * 创建一个带有指定错误信息的 {@link NotFoundException} 实例
     *
     * @param message 错误信息
     * @return {@link NotFoundException}
     */
    public static NotFoundException of(String message) {
        return new NotFoundException(message);
    }
}

