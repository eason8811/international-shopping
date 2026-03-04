package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * WebSocket 连接上下文值对象, 用于承载连接鉴权成功后的订阅信息
 *
 * @param actorUserId                连接操作者用户 ID
 * @param agent                      是否为管理侧坐席连接
 * @param subscribedTicketId         订阅工单 ID, 可为空
 * @param subscribedTicketNo         订阅工单编号, 可为空
 * @param resumeAfterMessageId       续传锚点消息 ID, 可为空
 * @param heartbeatIntervalSeconds   心跳间隔秒数
 * @param resumeTtlSeconds           续传窗口秒数
 */
public record TicketWsConnectionContext(@NotNull Long actorUserId,
                                        boolean agent,
                                        @Nullable Long subscribedTicketId,
                                        @Nullable String subscribedTicketNo,
                                        @Nullable Long resumeAfterMessageId,
                                        @NotNull Integer heartbeatIntervalSeconds,
                                        @NotNull Integer resumeTtlSeconds) {
}
