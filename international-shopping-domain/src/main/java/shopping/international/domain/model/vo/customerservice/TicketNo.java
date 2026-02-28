package shopping.international.domain.model.vo.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.domain.model.vo.NoGenerator;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单编号值对象, 对应表 `cs_ticket.ticket_no`
 */
@Getter
@ToString
@EqualsAndHashCode
public final class TicketNo {
    /**
     * 工单编号值
     */
    private final String value;

    /**
     * 构造工单编号值对象
     *
     * @param value 工单编号值
     */
    private TicketNo(String value) {
        this.value = value;
    }

    /**
     * 从字符串创建工单编号值对象
     *
     * @param raw 原始工单编号
     * @return 工单编号值对象
     */
    public static TicketNo of(String raw) {
        requireNotBlank(raw, "ticketNo 不能为空");
        String normalized = raw.strip();
        require(normalized.length() >= 10 && normalized.length() <= 32, "ticketNo 长度需在 10 到 32 之间");
        return new TicketNo(normalized);
    }

    /**
     * 生成新的工单编号值对象
     *
     * @return 工单编号值对象
     */
    public static TicketNo generate() {
        return new TicketNo(NoGenerator.generate());
    }
}
