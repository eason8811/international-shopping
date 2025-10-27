package shopping.international.types.exceptions;

/**
 * 参数错误异常
 */
public class IllegalParamException extends RuntimeException {
    public IllegalParamException(String message) {
        super(message);
    }
}
