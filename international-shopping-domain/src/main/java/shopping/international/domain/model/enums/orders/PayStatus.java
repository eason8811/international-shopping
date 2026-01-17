package shopping.international.domain.model.enums.orders;

/**
 * 支付状态
 * <ul>
 *     <li>{@code NONE} - 无支付状态</li>
 *     <li>{@code INIT} - 初始化</li>
 *     <li>{@code PENDING} - 正在支付</li>
 *     <li>{@code SUCCESS} - 支付成功</li>
 *     <li>{@code FAIL} - 支付失败</li>
 *     <li>{@code CLOSED} - 支付关闭</li>
 *     <li>{@code EXCEPTION} - 异常</li>
 * </ul>
 */
public enum PayStatus {
    NONE,
    INIT,
    PENDING,
    SUCCESS,
    FAIL,
    CLOSED,
    EXCEPTION
}
