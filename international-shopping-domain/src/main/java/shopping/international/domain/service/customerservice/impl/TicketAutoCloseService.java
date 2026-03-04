package shopping.international.domain.service.customerservice.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.customerservice.IAdminTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketActorType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.service.customerservice.ITicketAutoCloseService;

import java.time.LocalDateTime;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单自动关单领域服务实现, 供 customerservice 定时任务调用
 */
@Service
@RequiredArgsConstructor
public class TicketAutoCloseService implements ITicketAutoCloseService {

    /**
     * 自动关单扫描最大批次大小
     */
    private static final int MAX_BATCH_SIZE = 500;
    /**
     * 等待用户超时自动关闭日志备注
     */
    private static final String NOTE_USER_TIMEOUT = "USER_TIMEOUT";
    /**
     * 已解决超时自动关闭日志备注
     */
    private static final String NOTE_AUTO_CLOSE_AFTER_RESOLVE = "AUTO_CLOSE_AFTER_RESOLVE";

    /**
     * 管理侧工单仓储
     */
    private final IAdminTicketRepository adminTicketRepository;

    /**
     * 扫描并关闭等待用户超时的工单, 规则为 AWAITING_USER -> CLOSED
     *
     * @param updatedBefore 候选工单更新时间上限, 仅处理小于等于该时间的记录
     * @param limit 单批处理上限
     * @param sourceRef 任务运行标识, 将写入状态日志 source_ref 字段
     * @return 本次成功关闭的工单数量
     */
    @Override
    public int autoCloseAwaitingUserTickets(@NotNull LocalDateTime updatedBefore,
                                            int limit,
                                            @NotNull String sourceRef) {
        return autoCloseByStatus(
                TicketStatus.AWAITING_USER,
                updatedBefore,
                limit,
                sourceRef,
                NOTE_USER_TIMEOUT
        );
    }

    /**
     * 扫描并关闭已解决超时的工单, 规则为 RESOLVED -> CLOSED
     *
     * @param updatedBefore 候选工单更新时间上限, 仅处理小于等于该时间的记录
     * @param limit 单批处理上限
     * @param sourceRef 任务运行标识, 将写入状态日志 source_ref 字段
     * @return 本次成功关闭的工单数量
     */
    @Override
    public int autoCloseResolvedTickets(@NotNull LocalDateTime updatedBefore,
                                        int limit,
                                        @NotNull String sourceRef) {
        return autoCloseByStatus(
                TicketStatus.RESOLVED,
                updatedBefore,
                limit,
                sourceRef,
                NOTE_AUTO_CLOSE_AFTER_RESOLVE
        );
    }

    /**
     * 按状态批量执行自动关单
     *
     * @param fromStatus 候选状态
     * @param updatedBefore 更新时间上限
     * @param limit 单批处理上限
     * @param sourceRef 任务运行标识
     * @param note 状态日志备注
     * @return 成功关闭数量
     */
    private int autoCloseByStatus(@NotNull TicketStatus fromStatus,
                                  @NotNull LocalDateTime updatedBefore,
                                  int limit,
                                  @NotNull String sourceRef,
                                  @NotNull String note) {
        requireNotNull(fromStatus, "fromStatus 不能为空");
        requireNotNull(updatedBefore, "updatedBefore 不能为空");
        requireNotBlank(sourceRef, "sourceRef 不能为空");
        require(sourceRef.length() <= 128, "sourceRef 长度不能超过 128");
        int safeLimit = Math.min(Math.max(limit, 1), MAX_BATCH_SIZE);

        List<CustomerServiceTicket> candidateList = adminTicketRepository.listAutoCloseCandidatesByStatusAndUpdatedBefore(
                fromStatus,
                updatedBefore,
                safeLimit
        );
        if (candidateList.isEmpty())
            return 0;

        int closedCount = 0;
        for (CustomerServiceTicket ticket : candidateList) {
            if (ticket.getStatus() != fromStatus)
                continue;
            TicketStatus expectedFromStatus = ticket.getStatus();
            TicketStatusLog statusLog = ticket.transitionStatus(
                    TicketStatus.CLOSED,
                    TicketActorType.SCHEDULER,
                    null,
                    sourceRef,
                    note
            );
            boolean updated = adminTicketRepository.updateTicketStatusWithCasAndAppendLog(
                    ticket,
                    expectedFromStatus,
                    statusLog
            );
            if (updated)
                closedCount++;
        }
        return closedCount;
    }
}
