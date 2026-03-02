package shopping.international.domain.model.vo.customerservice;

import shopping.international.domain.model.enums.customerservice.TicketStatus;

import java.time.LocalDateTime;

/**
 * 用户侧创建工单结果值对象
 *
 * @param ticketId   工单 ID
 * @param ticketNo   工单编号
 * @param status     工单状态
 * @param createdAt  创建时间
 */
public record UserTicketCreateResult(Long ticketId,
                                     String ticketNo,
                                     TicketStatus status,
                                     LocalDateTime createdAt) {
}
