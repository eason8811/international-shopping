package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;

import java.time.LocalDateTime;

/**
 * 用户侧工单状态流转日志视图值对象
 *
 * @param id           日志 ID
 * @param ticketId     工单 ID
 * @param fromStatus   原状态
 * @param toStatus     目标状态
 * @param actorType    操作者类型
 * @param actorUserId  操作者用户 ID
 * @param sourceRef    来源引用 ID
 * @param note         备注
 * @param createdAt    创建时间
 */
public record UserTicketStatusLogView(Long id,
                                      Long ticketId,
                                      @Nullable TicketStatus fromStatus,
                                      TicketStatus toStatus,
                                      TicketActorType actorType,
                                      @Nullable Long actorUserId,
                                      @Nullable String sourceRef,
                                      @Nullable String note,
                                      LocalDateTime createdAt) {
}

