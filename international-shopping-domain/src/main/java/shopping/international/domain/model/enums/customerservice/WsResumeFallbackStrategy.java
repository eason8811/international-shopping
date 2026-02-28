package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 售后 WebSocket 续传降级策略枚举, 对齐 `CsWsResumeFallbackStrategy`
 * <ul>
 *     <li>{@code HTTP_CATCHUP_THEN_RECONNECT:} 先走 HTTP 补偿, 再重连</li>
 *     <li>{@code RECONNECT_WITHOUT_LAST_EVENT_ID:} 不带 lastEventId 直接重连</li>
 * </ul>
 */
public enum WsResumeFallbackStrategy {
    /**
     * 先走 HTTP 补偿, 再重连
     */
    HTTP_CATCHUP_THEN_RECONNECT,
    /**
     * 不带 lastEventId 直接重连
     */
    RECONNECT_WITHOUT_LAST_EVENT_ID;

    /**
     * 将字符串转换为 WebSocket 续传降级策略枚举
     *
     * @param value 原始字符串值
     * @return 对应的 WebSocket 续传降级策略枚举
     */
    public static WsResumeFallbackStrategy fromValue(String value) {
        requireNotBlank(value, "wsResumeFallbackStrategy 不能为空");
        return WsResumeFallbackStrategy.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
