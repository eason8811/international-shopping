package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单参与方角色枚举, 对齐表 `cs_ticket_participant.role`
 * <ul>
 *     <li>{@code OWNER:} 工单所有者</li>
 *     <li>{@code ASSIGNEE:} 工单指派处理人</li>
 *     <li>{@code COLLABORATOR:} 工单协作人</li>
 *     <li>{@code WATCHER:} 工单观察者</li>
 * </ul>
 */
public enum TicketParticipantRole {
    /**
     * 工单所有者
     */
    OWNER,
    /**
     * 工单指派处理人
     */
    ASSIGNEE,
    /**
     * 工单协作人
     */
    COLLABORATOR,
    /**
     * 工单观察者
     */
    WATCHER;

    /**
     * 将字符串转换为工单参与方角色枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单参与方角色枚举
     */
    public static TicketParticipantRole fromValue(String value) {
        requireNotBlank(value, "participantRole 不能为空");
        return TicketParticipantRole.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前角色是否允许执行工单状态流转
     *
     * @return 若允许执行状态流转, 返回 true
     */
    public boolean canTransitStatus() {
        return this == ASSIGNEE || this == COLLABORATOR;
    }
}
