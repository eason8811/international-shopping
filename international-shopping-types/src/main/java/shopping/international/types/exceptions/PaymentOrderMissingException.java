package shopping.international.types.exceptions;

/**
 * 订单关联支付单缺失异常
 *
 * <p>用于表示系统内应当存在但未找到的 payment_order 记录，属于数据一致性问题，通常需要告警与人工排查。</p>
 */
public class PaymentOrderMissingException extends AppException {

    /**
     * 构造异常
     *
     * @param message 错误信息
     */
    public PaymentOrderMissingException(String message) {
        super(message);
    }

    /**
     * 快速构造
     *
     * @param message 错误信息
     * @return 异常实例
     */
    public static PaymentOrderMissingException of(String message) {
        return new PaymentOrderMissingException(message);
    }
}

