package shopping.international.types.exceptions;

/**
 * <p>当尝试发送超过系统限制数量的邮件时抛出的异常, 此异常用于指示在给定的时间段内, 发送的邮件数量超过了允许的最大值</p>
 */
public class TooManyEmailSentException extends RuntimeException {
    public TooManyEmailSentException(String message) {
        super(message);
    }
}
