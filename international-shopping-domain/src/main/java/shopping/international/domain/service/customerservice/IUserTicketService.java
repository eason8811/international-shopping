package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧工单领域服务接口
 */
public interface IUserTicketService {

    /**
     * 分页查询当前用户工单列表
     *
     * @param userId       当前用户 ID
     * @param pageQuery    分页参数
     * @param status       工单状态筛选
     * @param issueType    问题类型筛选
     * @param orderNo      订单号筛选
     * @param shipmentNo   物流单号筛选
     * @param createdFrom  创建时间起始
     * @param createdTo    创建时间结束
     * @return 用户工单摘要分页结果
     */
    @NotNull
    PageResult<UserTicketSummaryView> listMyTickets(@NotNull Long userId,
                                                    @NotNull PageQuery pageQuery,
                                                    @Nullable TicketStatus status,
                                                    @Nullable TicketIssueType issueType,
                                                    @Nullable String orderNo,
                                                    @Nullable String shipmentNo,
                                                    @Nullable LocalDateTime createdFrom,
                                                    @Nullable LocalDateTime createdTo);

    /**
     * 创建用户工单
     *
     * @param userId          当前用户 ID
     * @param command         创建命令
     * @param idempotencyKey  幂等键
     * @return 创建结果
     */
    @NotNull
    UserTicketCreateResult createMyTicket(@NotNull Long userId,
                                          @NotNull TicketCreateCommand command,
                                          @NotNull String idempotencyKey);

    /**
     * 查询当前用户工单详情
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @return 工单详情
     */
    @NotNull
    UserTicketDetailView getMyTicketDetail(@NotNull Long userId,
                                           @NotNull TicketNo ticketNo);

    /**
     * 关闭当前用户工单
     *
     * @param userId          当前用户 ID
     * @param ticketNo        工单编号
     * @param note            关闭备注
     * @param idempotencyKey  幂等键
     * @return 关闭后的工单详情
     */
    @NotNull
    UserTicketDetailView closeMyTicket(@NotNull Long userId,
                                       @NotNull TicketNo ticketNo,
                                       @Nullable String note,
                                       @NotNull String idempotencyKey);

    /**
     * 查询当前用户指定工单的消息列表
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @param beforeId  向前翻页锚点
     * @param afterId   向后补偿锚点
     * @param ascOrder  是否按升序返回
     * @param size      返回条数
     * @return 消息列表
     */
    @NotNull
    List<TicketMessageView> listMyTicketMessages(@NotNull Long userId,
                                                 @NotNull TicketNo ticketNo,
                                                 @Nullable Long beforeId,
                                                 @Nullable Long afterId,
                                                 boolean ascOrder,
                                                 int size);

    /**
     * 发送当前用户的工单消息
     *
     * @param userId           当前用户 ID
     * @param ticketNo         工单编号
     * @param messageType      消息类型
     * @param content          消息正文
     * @param attachments      附件列表
     * @param clientMessageId  客户端消息幂等键
     * @param idempotencyKey   请求幂等键
     * @return 发送后的消息
     */
    @NotNull
    TicketMessageView createMyTicketMessage(@NotNull Long userId,
                                            @NotNull TicketNo ticketNo,
                                            @Nullable TicketMessageType messageType,
                                            @Nullable String content,
                                            @Nullable List<String> attachments,
                                            @NotNull String clientMessageId,
                                            @NotNull String idempotencyKey);

    /**
     * 编辑当前用户的工单消息
     *
     * @param userId          当前用户 ID
     * @param ticketNo        工单编号
     * @param messageNo       消息编号
     * @param content         新正文
     * @param idempotencyKey  请求幂等键
     * @return 编辑后的消息
     */
    @NotNull
    TicketMessageView editMyTicketMessage(@NotNull Long userId,
                                          @NotNull TicketNo ticketNo,
                                          @NotNull TicketMessageNo messageNo,
                                          @NotNull String content,
                                          @NotNull String idempotencyKey);

    /**
     * 撤回当前用户的工单消息
     *
     * @param userId          当前用户 ID
     * @param ticketNo        工单编号
     * @param messageNo       消息编号
     * @param reason          撤回原因
     * @param idempotencyKey  请求幂等键
     * @return 撤回后的消息
     */
    @NotNull
    TicketMessageView recallMyTicketMessage(@NotNull Long userId,
                                            @NotNull TicketNo ticketNo,
                                            @NotNull TicketMessageNo messageNo,
                                            @Nullable String reason,
                                            @NotNull String idempotencyKey);

    /**
     * 标记当前用户在工单下的消息已读位点
     *
     * @param userId             当前用户 ID
     * @param ticketNo           工单编号
     * @param lastReadMessageId  最后已读消息 ID
     * @param idempotencyKey     请求幂等键
     * @return 已读位点更新结果
     */
    @NotNull
    TicketReadUpdateView markMyTicketRead(@NotNull Long userId,
                                          @NotNull TicketNo ticketNo,
                                          @NotNull Long lastReadMessageId,
                                          @NotNull String idempotencyKey);

    /**
     * 分页查询当前用户可见的工单状态日志
     *
     * @param userId     当前用户 ID
     * @param ticketNo   工单编号
     * @param pageQuery  分页参数
     * @return 状态日志分页结果
     */
    @NotNull
    PageResult<UserTicketStatusLogView> listMyTicketStatusLogs(@NotNull Long userId,
                                                               @NotNull TicketNo ticketNo,
                                                               @NotNull PageQuery pageQuery);

    /**
     * 查询当前用户工单关联的补发物流列表
     *
     * @param userId    当前用户 ID
     * @param ticketNo  工单编号
     * @return 物流摘要列表
     */
    @NotNull
    List<UserTicketShipmentSummaryView> listMyTicketReshipShipments(@NotNull Long userId,
                                                                    @NotNull TicketNo ticketNo);

    /**
     * 创建当前用户的 WebSocket 会话签发结果
     *
     * @param userId          当前用户 ID
     * @param command         会话创建命令
     * @param idempotencyKey  请求幂等键
     * @return 会话签发结果
     */
    @NotNull
    TicketWsSessionIssueView createMyWsSession(@NotNull Long userId,
                                               @NotNull TicketWsSessionCreateCommand command,
                                               @NotNull String idempotencyKey);
}
