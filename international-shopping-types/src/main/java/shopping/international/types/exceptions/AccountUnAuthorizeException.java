package shopping.international.types.exceptions;

/**
 * 账户未激活
 */
public class AccountUnAuthorizeException extends AccountException {
    public AccountUnAuthorizeException(String message) {
        super(message);
    }
}
