package shopping.international.trigger.job.customerservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shopping.international.domain.service.customerservice.ITicketAutoCloseService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 等待用户超时自动关单任务, 规则为 AWAITING_USER -> CLOSED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwaitingUserTicketAutoCloseJob {

    /**
     * 任务运行时间格式
     */
    private static final DateTimeFormatter RUN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /**
     * source_ref 前缀
     */
    private static final String SOURCE_REF_PREFIX = "customerservice:auto-close:awaiting-user:";

    /**
     * 工单自动关单领域服务
     */
    private final ITicketAutoCloseService ticketAutoCloseService;

    /**
     * 单批处理数量
     */
    @Value("${customerservice.auto-close.awaiting-user.batch-size:100}")
    private int batchSize;
    /**
     * 等待用户超时阈值, 默认 3 天
     */
    @Value("${customerservice.auto-close.awaiting-user.timeout:3d}")
    private Duration timeout;

    /**
     * 定时执行等待用户超时自动关单
     */
    @Scheduled(fixedDelayString = "${customerservice.auto-close.awaiting-user.fixed-delay:15m}")
    public void autoCloseAwaitingUserTickets() {
        try {
            LocalDateTime updatedBefore = LocalDateTime.now().minus(timeout);
            String runId = buildRunId();
            int closedCount = ticketAutoCloseService.autoCloseAwaitingUserTickets(updatedBefore, batchSize, runId);
            if (closedCount > 0) {
                log.info("等待用户超时自动关单完成, closedCount: {}, updatedBefore: {}, runId: {}",
                        closedCount, updatedBefore, runId);
            }
        } catch (Exception exception) {
            log.error("等待用户超时自动关单任务执行失败, err: {}", exception.getMessage(), exception);
        }
    }

    /**
     * 构建任务运行标识, 写入状态日志 source_ref
     *
     * @return 任务运行标识
     */
    private String buildRunId() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return SOURCE_REF_PREFIX + RUN_TIME_FORMATTER.format(LocalDateTime.now()) + ":" + suffix;
    }
}
