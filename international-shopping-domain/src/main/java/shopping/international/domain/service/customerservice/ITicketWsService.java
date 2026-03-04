package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.customerservice.TicketMessageView;
import shopping.international.domain.model.vo.customerservice.TicketWsConnectionContext;

import java.util.List;

/**
 * 工单 WebSocket 领域服务接口
 */
public interface ITicketWsService {

    /**
     * 校验 WebSocket 连接请求并返回连接上下文
     *
     * @param wsToken     WebSocket 短期令牌
     * @param ticketNo    订阅工单编号, 可为空
     * @param ticketId    订阅工单 ID, 可为空
     * @param lastEventId 续传锚点事件 ID, 可为空
     * @return 连接上下文
     */
    @NotNull
    TicketWsConnectionContext authorizeConnection(@NotNull String wsToken,
                                                  @Nullable String ticketNo,
                                                  @Nullable Long ticketId,
                                                  @Nullable String lastEventId);

    /**
     * 拉取单工单范围内的增量消息
     *
     * @param actorUserId   连接操作者用户 ID
     * @param agent         是否管理侧坐席连接
     * @param ticketId      工单 ID
     * @param afterMessageId 增量锚点消息 ID, 可为空
     * @param size          拉取条数
     * @return 增量消息列表
     */
    @NotNull
    List<TicketMessageView> listIncrementalMessages(@NotNull Long actorUserId,
                                                    boolean agent,
                                                    @NotNull Long ticketId,
                                                    @Nullable Long afterMessageId,
                                                    int size);
}
