package shopping.international.domain.model.enums.customerservice;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 工单问题类型枚举, 对齐表 `cs_ticket.issue_type`
 * <ul>
 *     <li>{@code REFUND:} 退款问题</li>
 *     <li>{@code RESHIP:} 补寄问题</li>
 *     <li>{@code CLAIM:} 理赔问题</li>
 *     <li>{@code DELIVERY:} 物流问题</li>
 *     <li>{@code ADDRESS:} 改址问题</li>
 *     <li>{@code PRODUCT:} 商品问题</li>
 *     <li>{@code PAYMENT:} 支付问题</li>
 *     <li>{@code OTHER:} 其他问题</li>
 * </ul>
 */
public enum TicketIssueType {
    /**
     * 退款问题
     */
    REFUND,
    /**
     * 补寄问题
     */
    RESHIP,
    /**
     * 理赔问题
     */
    CLAIM,
    /**
     * 物流问题
     */
    DELIVERY,
    /**
     * 改址问题
     */
    ADDRESS,
    /**
     * 商品问题
     */
    PRODUCT,
    /**
     * 支付问题
     */
    PAYMENT,
    /**
     * 其他问题
     */
    OTHER;

    /**
     * 将字符串转换为工单问题类型枚举
     *
     * @param value 原始字符串值
     * @return 对应的工单问题类型枚举
     */
    public static TicketIssueType fromValue(String value) {
        requireNotBlank(value, "issueType 不能为空");
        return TicketIssueType.valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
