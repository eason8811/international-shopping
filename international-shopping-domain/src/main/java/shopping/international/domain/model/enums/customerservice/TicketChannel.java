package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单来源渠道枚举, 对齐表 `cs_ticket.channel`
 * <ul>
 *     <li>{@code CLIENT:} 客户端来源</li>
 *     <li>{@code ADMIN:} 管理后台来源</li>
 *     <li>{@code API:} 开放接口来源</li>
 *     <li>{@code SYSTEM:} 系统任务来源</li>
 * </ul>
 */
public enum TicketChannel {
    /**
     * 客户端来源
     */
    CLIENT,
    /**
     * 管理后台来源
     */
    ADMIN,
    /**
     * 开放接口来源
     */
    API,
    /**
     * 系统任务来源
     */
    SYSTEM;

    /**
     * 将字符串转换为工单来源渠道枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单来源渠道枚举
     */
    public static TicketChannel fromValue(String value) {
        requireNotBlank(value, "channel 不能为空");
        return TicketChannel.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
