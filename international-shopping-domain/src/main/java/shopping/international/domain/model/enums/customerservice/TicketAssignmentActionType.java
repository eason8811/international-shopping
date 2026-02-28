package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单指派动作枚举, 对齐表 `cs_ticket_assignment_log.action_type`
 * <ul>
 *     <li>{@code ASSIGN:} 指派</li>
 *     <li>{@code REASSIGN:} 改派</li>
 *     <li>{@code UNASSIGN:} 取消指派</li>
 *     <li>{@code AUTO_ASSIGN:} 自动指派</li>
 * </ul>
 */
public enum TicketAssignmentActionType {
    /**
     * 指派
     */
    ASSIGN,
    /**
     * 改派
     */
    REASSIGN,
    /**
     * 取消指派
     */
    UNASSIGN,
    /**
     * 自动指派
     */
    AUTO_ASSIGN;

    /**
     * 将字符串转换为工单指派动作枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单指派动作枚举
     */
    public static TicketAssignmentActionType fromValue(String value) {
        requireNotBlank(value, "actionType 不能为空");
        return TicketAssignmentActionType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前动作是否要求必须提供目标指派人
     *
     * @return 若要求提供目标指派人, 返回 true
     */
    public boolean requiresAssignee() {
        return this == ASSIGN || this == REASSIGN || this == AUTO_ASSIGN;
    }
}
