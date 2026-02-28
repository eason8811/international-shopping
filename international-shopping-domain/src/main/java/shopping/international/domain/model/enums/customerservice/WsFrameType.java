package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 售后 WebSocket 服务端帧类型枚举
 * <ul>
 *     <li>{@code EVENT:} 事件帧</li>
 *     <li>{@code ERROR:} 错误帧</li>
 * </ul>
 */
public enum WsFrameType {
    /**
     * 事件帧
     */
    EVENT,
    /**
     * 错误帧
     */
    ERROR;

    /**
     * 将字符串转换为 WebSocket 帧类型枚举, 支持 `event` 或 `error`
     *
     * @param value 原始字符串值
     * @return 对应的 WebSocket 帧类型枚举
     */
    public static WsFrameType fromValue(String value) {
        requireNotBlank(value, "wsFrameType 不能为空");
        return WsFrameType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
