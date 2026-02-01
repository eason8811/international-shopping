package shopping.international.types.exceptions;

/**
 * 参数错误异常
 */
public class IllegalParamException extends RuntimeException {
    public IllegalParamException(String message) {
        super(message);
    }

    /**
     * 创建一个带有指定错误信息的 <code>IllegalParamException</code> 实例
     *
     * @param message 错误信息, 用于描述异常发生的原因或上下文
     * @return 包含给定错误信息的 <code>IllegalParamException</code> 实例
     */
    public static IllegalParamException of(String message) {
        return new IllegalParamException(message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public IllegalParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
