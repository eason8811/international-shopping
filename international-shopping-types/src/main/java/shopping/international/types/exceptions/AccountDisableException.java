package shopping.international.types.exceptions;

/**
 * 账户不可用
 */
public class AccountDisableException extends AccountException {
    public AccountDisableException(String message) {
        super(message);
    }
}
