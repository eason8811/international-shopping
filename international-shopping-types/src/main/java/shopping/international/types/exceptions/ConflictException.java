package shopping.international.types.exceptions;

/**
 * <p>当请求尝试创建或更新资源时, 由于与现有资源发生冲突而抛出的异常</p>
 */
public class ConflictException extends RuntimeException {
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
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConflictException(String message) {
        super(message);
    }
}
