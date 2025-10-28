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
}
