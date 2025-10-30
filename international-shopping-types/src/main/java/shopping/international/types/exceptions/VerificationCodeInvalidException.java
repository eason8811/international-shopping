package shopping.international.types.exceptions;

/**
 * 验证码无效异常
 */
public class VerificationCodeInvalidException extends RuntimeException {
    public VerificationCodeInvalidException(String message) {
        super(message);
    }
}
