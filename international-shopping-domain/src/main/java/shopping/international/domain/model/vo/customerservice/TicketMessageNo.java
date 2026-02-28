package shopping.international.domain.model.vo.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import shopping.international.domain.model.vo.NoGenerator;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单消息编号值对象, 对应表 `cs_ticket_message.message_no`
 */
@Getter
@ToString
@EqualsAndHashCode
public final class TicketMessageNo {
    /**
     * 工单消息编号值
     */
    private final String value;

    /**
     * 构造工单消息编号值对象
     *
     * @param value 工单消息编号值
     */
    private TicketMessageNo(String value) {
        this.value = value;
    }

    /**
     * 从字符串创建工单消息编号值对象
     *
     * @param raw 原始工单消息编号
     * @return 工单消息编号值对象
     */
    public static TicketMessageNo of(String raw) {
        requireNotBlank(raw, "messageNo 不能为空");
        String normalized = raw.strip();
        require(normalized.length() >= 10 && normalized.length() <= 32, "messageNo 长度需在 10 到 32 之间");
        return new TicketMessageNo(normalized);
    }

    /**
     * 生成新的工单消息编号值对象
     *
     * @return 工单消息编号值对象
     */
    public static TicketMessageNo generate() {
        return new TicketMessageNo(NoGenerator.generate());
    }
}
