package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 售后 WebSocket 错误码枚举, 对齐 `CsWsErrorCode`
 * <ul>
 *     <li>{@code INVALID_FRAME:} 帧格式非法</li>
 *     <li>{@code UNAUTHORIZED:} 未认证</li>
 *     <li>{@code FORBIDDEN:} 无权限</li>
 *     <li>{@code RATE_LIMITED:} 限流</li>
 *     <li>{@code RESUME_EXPIRED:} 续传窗口已过期</li>
 *     <li>{@code RESUME_NOT_FOUND:} 续传锚点不存在</li>
 *     <li>{@code RESUME_NOT_AVAILABLE:} 当前连接不支持续传</li>
 * </ul>
 */
public enum WsErrorCode {
    /**
     * 帧格式非法
     */
    INVALID_FRAME,
    /**
     * 未认证
     */
    UNAUTHORIZED,
    /**
     * 无权限
     */
    FORBIDDEN,
    /**
     * 限流
     */
    RATE_LIMITED,
    /**
     * 续传窗口已过期
     */
    RESUME_EXPIRED,
    /**
     * 续传锚点不存在
     */
    RESUME_NOT_FOUND,
    /**
     * 当前连接不支持续传
     */
    RESUME_NOT_AVAILABLE;

    /**
     * 将字符串转换为 WebSocket 错误码枚举
     *
     * @param value 原始字符串值
     * @return 对应的 WebSocket 错误码枚举
     */
    public static WsErrorCode fromValue(String value) {
        requireNotBlank(value, "wsErrorCode 不能为空");
        return WsErrorCode.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
