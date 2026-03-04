package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * 工单自动关单领域服务接口, 提供定时任务使用的自动状态推进能力
 */
public interface ITicketAutoCloseService {

    /**
     * 扫描并关闭等待用户超时的工单, 规则为 AWAITING_USER -> CLOSED
     *
     * @param updatedBefore 候选工单更新时间上限, 仅处理小于等于该时间的记录
     * @param limit 单批处理上限
     * @param sourceRef 任务运行标识, 将写入状态日志 source_ref 字段
     * @return 本次成功关闭的工单数量
     */
    int autoCloseAwaitingUserTickets(@NotNull LocalDateTime updatedBefore,
                                     int limit,
                                     @NotNull String sourceRef);

    /**
     * 扫描并关闭已解决超时的工单, 规则为 RESOLVED -> CLOSED
     *
     * @param updatedBefore 候选工单更新时间上限, 仅处理小于等于该时间的记录
     * @param limit 单批处理上限
     * @param sourceRef 任务运行标识, 将写入状态日志 source_ref 字段
     * @return 本次成功关闭的工单数量
     */
    int autoCloseResolvedTickets(@NotNull LocalDateTime updatedBefore,
                                 int limit,
                                 @NotNull String sourceRef);
}
