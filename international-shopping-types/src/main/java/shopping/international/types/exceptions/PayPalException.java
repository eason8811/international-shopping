package shopping.international.types.exceptions;

/**
 * PayPal 支付异常
 */
public class PayPalException extends AppException {
    public PayPalException(String message) {
        super(message);
    }

    public PayPalException(String message, Throwable cause) {
        super(message, cause);
    }
}
