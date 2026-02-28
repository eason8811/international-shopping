package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单优先级枚举, 对齐表 `cs_ticket.priority`
 * <ul>
 *     <li>{@code LOW:} 低优先级</li>
 *     <li>{@code NORMAL:} 普通优先级</li>
 *     <li>{@code HIGH:} 高优先级</li>
 *     <li>{@code URGENT:} 紧急优先级</li>
 * </ul>
 */
public enum TicketPriority {
    /**
     * 低优先级
     */
    LOW,
    /**
     * 普通优先级
     */
    NORMAL,
    /**
     * 高优先级
     */
    HIGH,
    /**
     * 紧急优先级
     */
    URGENT;

    /**
     * 将字符串转换为工单优先级枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单优先级枚举
     */
    public static TicketPriority fromValue(String value) {
        requireNotBlank(value, "priority 不能为空");
        return TicketPriority.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前优先级是否属于升级优先级
     *
     * @return 若为 HIGH 或 URGENT, 返回 true
     */
    public boolean isEscalated() {
        return this == HIGH || this == URGENT;
    }
}
