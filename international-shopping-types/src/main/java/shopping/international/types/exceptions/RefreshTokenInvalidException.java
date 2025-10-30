package shopping.international.types.exceptions;

/**
 * 刷新 Token 无效异常
 */
public class RefreshTokenInvalidException extends RuntimeException {
    public RefreshTokenInvalidException(String message) {
        super(message);
    }
}
