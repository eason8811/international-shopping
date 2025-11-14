package shopping.international.types.exceptions;

/**
 * 幂等性错误
 */
public class IdempotencyException extends RuntimeException {
    public IdempotencyException(String message) {
        super(message);
    }
}
