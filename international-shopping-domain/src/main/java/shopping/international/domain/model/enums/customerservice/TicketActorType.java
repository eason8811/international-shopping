package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单操作人类型枚举, 对齐表 `cs_ticket_status_log.actor_type` 和 `cs_ticket_assignment_log.actor_type`
 * <ul>
 *     <li>{@code USER:} 用户操作</li>
 *     <li>{@code AGENT:} 客服操作</li>
 *     <li>{@code MERCHANT:} 商家操作</li>
 *     <li>{@code SYSTEM:} 系统操作</li>
 *     <li>{@code SCHEDULER:} 调度任务操作</li>
 *     <li>{@code API:} 开放接口操作</li>
 * </ul>
 */
public enum TicketActorType {
    /**
     * 用户操作
     */
    USER,
    /**
     * 客服操作
     */
    AGENT,
    /**
     * 商家操作
     */
    MERCHANT,
    /**
     * 系统操作
     */
    SYSTEM,
    /**
     * 调度任务操作
     */
    SCHEDULER,
    /**
     * 开放接口操作
     */
    API;

    /**
     * 将字符串转换为工单操作人类型枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单操作人类型枚举
     */
    public static TicketActorType fromValue(String value) {
        requireNotBlank(value, "actorType 不能为空");
        return TicketActorType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }

    /**
     * 判断当前操作人是否为系统自动行为
     *
     * @return 若为系统自动行为, 返回 true
     */
    public boolean isSystemActor() {
        return this == SYSTEM || this == SCHEDULER;
    }
}
