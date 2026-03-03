package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketAssignmentActionType;
import shopping.international.domain.model.enums.customerservice.TicketMessageType;
import shopping.international.domain.model.enums.customerservice.TicketParticipantRole;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketPriority;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理侧工单领域服务接口
 */
public interface IAdminTicketService {

    /**
     * 分页查询管理侧工单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 工单摘要分页结果
     */
    @NotNull
    PageResult<AdminTicketSummaryView> pageTickets(@NotNull AdminTicketPageCriteria criteria,
                                                   @NotNull PageQuery pageQuery);

    /**
     * 查询管理侧工单详情
     *
     * @param ticketId 工单 ID
     * @return 工单详情视图
     */
    @NotNull
    AdminTicketDetailView getTicketDetail(@NotNull Long ticketId);

    /**
     * 更新工单元数据
     *
     * @param actorUserId            操作者用户 ID
     * @param ticketId               工单 ID
     * @param priority               工单优先级
     * @param tags                   工单标签
     * @param requestedRefundAmount  申请退款金额
     * @param currency               币种
     * @param claimExternalId        理赔外部编号
     * @param slaDueAt               SLA 到期时间
     * @param idempotencyKey         幂等键
     * @return 更新后的工单详情视图
     */
    @NotNull
    AdminTicketDetailView patchTicket(@NotNull Long actorUserId,
                                      @NotNull Long ticketId,
                                      @Nullable TicketPriority priority,
                                      @Nullable List<String> tags,
                                      @Nullable Long requestedRefundAmount,
                                      @Nullable String currency,
                                      @Nullable String claimExternalId,
                                      @Nullable LocalDateTime slaDueAt,
                                      @NotNull String idempotencyKey);

    /**
     * 指派或转派工单
     *
     * @param actorUserId       操作者用户 ID
     * @param ticketId          工单 ID
     * @param toAssigneeUserId  目标指派坐席用户 ID
     * @param actionType        指派动作
     * @param note              备注
     * @param sourceRef         来源引用 ID
     * @param idempotencyKey    幂等键
     * @return 更新后的工单详情视图
     */
    @NotNull
    AdminTicketDetailView assignTicket(@NotNull Long actorUserId,
                                       @NotNull Long ticketId,
                                       @Nullable Long toAssigneeUserId,
                                       @NotNull TicketAssignmentActionType actionType,
                                       @Nullable String note,
                                       @Nullable String sourceRef,
                                       @NotNull String idempotencyKey);

    /**
     * 推进工单状态
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param toStatus        目标状态
     * @param note            备注
     * @param sourceRef       来源引用 ID
     * @param idempotencyKey  幂等键
     * @return 更新后的工单详情视图
     */
    @NotNull
    AdminTicketDetailView transitionTicketStatus(@NotNull Long actorUserId,
                                                 @NotNull Long ticketId,
                                                 @NotNull TicketStatus toStatus,
                                                 @Nullable String note,
                                                 @Nullable String sourceRef,
                                                 @NotNull String idempotencyKey);

    /**
     * 查询管理侧指定工单的消息列表
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @param beforeId    向前翻页锚点
     * @param afterId     向后补偿锚点
     * @param ascOrder    是否按升序返回
     * @param size        返回条数
     * @return 消息列表
     */
    @NotNull
    List<TicketMessageView> listTicketMessages(@NotNull Long actorUserId,
                                               @NotNull Long ticketId,
                                               @Nullable Long beforeId,
                                               @Nullable Long afterId,
                                               boolean ascOrder,
                                               int size);

    /**
     * 发送管理侧工单消息
     *
     * @param actorUserId      操作者用户 ID
     * @param ticketId         工单 ID
     * @param messageType      消息类型
     * @param content          消息正文
     * @param attachments      附件列表
     * @param clientMessageId  客户端消息幂等键
     * @param idempotencyKey   请求幂等键
     * @return 发送后的消息
     */
    @NotNull
    TicketMessageView createTicketMessage(@NotNull Long actorUserId,
                                          @NotNull Long ticketId,
                                          @Nullable TicketMessageType messageType,
                                          @Nullable String content,
                                          @Nullable List<String> attachments,
                                          @NotNull String clientMessageId,
                                          @NotNull String idempotencyKey);

    /**
     * 编辑管理侧工单消息
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param messageId       消息 ID
     * @param content         新正文
     * @param idempotencyKey  请求幂等键
     * @return 编辑后的消息
     */
    @NotNull
    TicketMessageView editTicketMessage(@NotNull Long actorUserId,
                                        @NotNull Long ticketId,
                                        @NotNull Long messageId,
                                        @NotNull String content,
                                        @NotNull String idempotencyKey);

    /**
     * 撤回管理侧工单消息
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param messageId       消息 ID
     * @param reason          撤回原因
     * @param idempotencyKey  请求幂等键
     * @return 撤回后的消息
     */
    @NotNull
    TicketMessageView recallTicketMessage(@NotNull Long actorUserId,
                                          @NotNull Long ticketId,
                                          @NotNull Long messageId,
                                          @Nullable String reason,
                                          @NotNull String idempotencyKey);

    /**
     * 标记管理侧工单消息已读位点
     *
     * @param actorUserId        操作者用户 ID
     * @param ticketId           工单 ID
     * @param lastReadMessageId  最后已读消息 ID
     * @param idempotencyKey     请求幂等键
     * @return 已读位点更新结果
     */
    @NotNull
    TicketReadUpdateView markTicketRead(@NotNull Long actorUserId,
                                        @NotNull Long ticketId,
                                        @NotNull Long lastReadMessageId,
                                        @NotNull String idempotencyKey);

    /**
     * 查询管理侧工单参与方列表
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @return 参与方列表
     */
    @NotNull
    List<TicketParticipant> listTicketParticipants(@NotNull Long actorUserId,
                                                   @NotNull Long ticketId);

    /**
     * 新增管理侧工单参与方
     *
     * @param actorUserId       操作者用户 ID
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @param role              参与方角色
     * @param idempotencyKey    请求幂等键
     * @return 新增后的参与方
     */
    @NotNull
    TicketParticipant createTicketParticipant(@NotNull Long actorUserId,
                                              @NotNull Long ticketId,
                                              @NotNull TicketParticipantType participantType,
                                              @Nullable Long participantUserId,
                                              @NotNull TicketParticipantRole role,
                                              @NotNull String idempotencyKey);

    /**
     * 更新管理侧工单参与方角色
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param participantId   参与方 ID
     * @param role            目标角色
     * @param idempotencyKey  请求幂等键
     * @return 更新后的参与方
     */
    @NotNull
    TicketParticipant patchTicketParticipant(@NotNull Long actorUserId,
                                             @NotNull Long ticketId,
                                             @NotNull Long participantId,
                                             @NotNull TicketParticipantRole role,
                                             @NotNull String idempotencyKey);

    /**
     * 将管理侧工单参与方设为离开
     *
     * @param actorUserId     操作者用户 ID
     * @param ticketId        工单 ID
     * @param participantId   参与方 ID
     * @param idempotencyKey  请求幂等键
     */
    void leaveTicketParticipant(@NotNull Long actorUserId,
                                @NotNull Long ticketId,
                                @NotNull Long participantId,
                                @NotNull String idempotencyKey);

    /**
     * 分页查询管理侧工单状态日志
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @param pageQuery   分页参数
     * @return 状态日志分页结果
     */
    @NotNull
    PageResult<TicketStatusLog> listTicketStatusLogs(@NotNull Long actorUserId,
                                                     @NotNull Long ticketId,
                                                     @NotNull PageQuery pageQuery);

    /**
     * 分页查询管理侧工单指派日志
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketId    工单 ID
     * @param pageQuery   分页参数
     * @return 指派日志分页结果
     */
    @NotNull
    PageResult<TicketAssignmentLog> listTicketAssignmentLogs(@NotNull Long actorUserId,
                                                             @NotNull Long ticketId,
                                                             @NotNull PageQuery pageQuery);

    /**
     * 创建管理侧 WebSocket 会话签发结果
     *
     * @param actorUserId     操作者用户 ID
     * @param command         会话创建命令
     * @param idempotencyKey  请求幂等键
     * @return 会话签发结果
     */
    @NotNull
    TicketWsSessionIssueView createWsSession(@NotNull Long actorUserId,
                                             @NotNull TicketWsSessionCreateCommand command,
                                             @NotNull String idempotencyKey);
}
