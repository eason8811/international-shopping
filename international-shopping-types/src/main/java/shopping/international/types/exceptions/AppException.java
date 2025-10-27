package shopping.international.types.exceptions;

/**
 * 系统内异常
 */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }
}
