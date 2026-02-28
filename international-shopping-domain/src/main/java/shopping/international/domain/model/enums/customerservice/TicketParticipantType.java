package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单参与方类型枚举, 对齐表 `cs_ticket_participant.participant_type`
 * <ul>
 *     <li>{@code USER:} 用户参与方</li>
 *     <li>{@code AGENT:} 客服参与方</li>
 *     <li>{@code MERCHANT:} 商家参与方</li>
 *     <li>{@code SYSTEM:} 系统参与方</li>
 *     <li>{@code BOT:} 机器人参与方</li>
 * </ul>
 */
public enum TicketParticipantType {
    /**
     * 用户参与方
     */
    USER,
    /**
     * 客服参与方
     */
    AGENT,
    /**
     * 商家参与方
     */
    MERCHANT,
    /**
     * 系统参与方
     */
    SYSTEM,
    /**
     * 机器人参与方
     */
    BOT;

    /**
     * 将字符串转换为工单参与方类型枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单参与方类型枚举
     */
    public static TicketParticipantType fromValue(String value) {
        requireNotBlank(value, "participantType 不能为空");
        return TicketParticipantType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前参与方类型是否要求必须具备用户 ID
     *
     * @return 若必须具备用户 ID, 返回 true
     */
    public boolean requiresUserId() {
        return this == USER || this == AGENT || this == MERCHANT;
    }
}
