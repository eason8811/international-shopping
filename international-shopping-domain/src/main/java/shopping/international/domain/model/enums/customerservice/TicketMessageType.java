package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单消息类型枚举, 对齐表 `cs_ticket_message.message_type`
 * <ul>
 *     <li>{@code TEXT:} 文本消息</li>
 *     <li>{@code IMAGE:} 图片消息</li>
 *     <li>{@code FILE:} 文件消息</li>
 *     <li>{@code SYSTEM_EVENT:} 系统事件消息</li>
 *     <li>{@code RICH:} 富文本消息</li>
 * </ul>
 */
public enum TicketMessageType {
    /**
     * 文本消息
     */
    TEXT,
    /**
     * 图片消息
     */
    IMAGE,
    /**
     * 文件消息
     */
    FILE,
    /**
     * 系统事件消息
     */
    SYSTEM_EVENT,
    /**
     * 富文本消息
     */
    RICH;

    /**
     * 将字符串转换为工单消息类型枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单消息类型枚举
     */
    public static TicketMessageType fromValue(String value) {
        requireNotBlank(value, "messageType 不能为空");
        return TicketMessageType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前消息类型是否建议携带正文内容
     *
     * @return 若建议携带正文内容, 返回 true
     */
    public boolean prefersTextContent() {
        return this == TEXT || this == SYSTEM_EVENT || this == RICH;
    }
}
