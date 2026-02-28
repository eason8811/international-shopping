package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 售后 WebSocket 事件类型枚举, 对齐 `CsWsEventType`
 * <ul>
 *     <li>{@code WS_CONNECTED:} 建连成功事件</li>
 *     <li>{@code MESSAGE_CREATED:} 消息创建事件</li>
 *     <li>{@code MESSAGE_UPDATED:} 消息编辑事件</li>
 *     <li>{@code MESSAGE_RECALLED:} 消息撤回事件</li>
 *     <li>{@code TICKET_READ_UPDATED:} 已读位点变更事件</li>
 *     <li>{@code TICKET_STATUS_CHANGED:} 工单状态变更事件</li>
 *     <li>{@code TICKET_ASSIGNMENT_CHANGED:} 工单指派变更事件</li>
 * </ul>
 */
public enum WsEventType {
    /**
     * 建连成功事件
     */
    WS_CONNECTED,
    /**
     * 消息创建事件
     */
    MESSAGE_CREATED,
    /**
     * 消息编辑事件
     */
    MESSAGE_UPDATED,
    /**
     * 消息撤回事件
     */
    MESSAGE_RECALLED,
    /**
     * 已读位点变更事件
     */
    TICKET_READ_UPDATED,
    /**
     * 工单状态变更事件
     */
    TICKET_STATUS_CHANGED,
    /**
     * 工单指派变更事件
     */
    TICKET_ASSIGNMENT_CHANGED;

    /**
     * 将字符串转换为 WebSocket 事件类型枚举
     *
     * @param value 原始字符串值
     * @return 对应的 WebSocket 事件类型枚举
     */
    public static WsEventType fromValue(String value) {
        requireNotBlank(value, "eventType 不能为空");
        return WsEventType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
