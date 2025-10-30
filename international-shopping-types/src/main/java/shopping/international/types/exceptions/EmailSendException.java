package shopping.international.types.exceptions;

/**
 * 邮件发送异常
 */
public class EmailSendException extends RuntimeException {
    public EmailSendException(String message) {
        super(message);
    }
}
