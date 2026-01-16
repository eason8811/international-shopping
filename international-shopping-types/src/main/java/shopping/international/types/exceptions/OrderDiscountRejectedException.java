package shopping.international.types.exceptions;

/**
 * <p>创建订单时折扣码不可用/不满足条件导致的异常</p>
 */
public class OrderDiscountRejectedException extends RuntimeException {
    public OrderDiscountRejectedException(String message) {
        super(message);
    }
}
