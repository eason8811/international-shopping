package shopping.international.types.exceptions;

/**
 * 折扣失败异常
 */
public class DiscountFailureException extends RuntimeException {
    public DiscountFailureException(String message) {
        super(message);
    }
}
