package shopping.international.domain.adapter.repository.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketPageCriteria;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;
import shopping.international.domain.model.vo.customerservice.TicketMessageView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 管理侧工单仓储接口
 */
public interface IAdminTicketRepository {

    /**
     * 分页查询管理侧工单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 工单摘要分页结果
     */
    @NotNull
    PageResult<AdminTicketSummaryView> pageAdminTicketSummaries(@NotNull AdminTicketPageCriteria criteria,
                                                                @NotNull PageQuery pageQuery);

    /**
     * 按工单 ID 查询管理侧工单详情
     *
     * @param ticketId 工单 ID
     * @return 工单详情视图
     */
    @NotNull
    Optional<AdminTicketDetailView> findAdminTicketDetail(@NotNull Long ticketId);

    /**
     * 按工单 ID 查询工单聚合
     *
     * @param ticketId 工单 ID
     * @return 工单聚合
     */
    @NotNull
    Optional<CustomerServiceTicket> findByTicketId(@NotNull Long ticketId);

    /**
     * 基于更新时间 CAS 更新工单元数据
     *
     * @param ticket             已完成元数据变更的工单聚合
     * @param expectedUpdatedAt  期望旧更新时间
     * @return 更新是否成功
     */
    boolean updateTicketMetadataWithCas(@NotNull CustomerServiceTicket ticket,
                                        @NotNull LocalDateTime expectedUpdatedAt);

    /**
     * 基于更新时间 CAS 更新工单指派信息, 并写入指派日志
     *
     * @param ticket             已完成指派变更的工单聚合
     * @param expectedUpdatedAt  期望旧更新时间
     * @param assignmentLog      指派日志实体
     * @return 更新是否成功
     */
    boolean updateTicketAssignmentWithCasAndAppendLog(@NotNull CustomerServiceTicket ticket,
                                                      @NotNull LocalDateTime expectedUpdatedAt,
                                                      @NotNull TicketAssignmentLog assignmentLog);

    /**
     * 基于状态 CAS 更新工单状态, 并写入状态日志
     *
     * @param ticket               已完成状态推进的工单聚合
     * @param expectedFromStatus   期望旧状态
     * @param statusLog            状态日志实体
     * @return 更新是否成功
     */
    boolean updateTicketStatusWithCasAndAppendLog(@NotNull CustomerServiceTicket ticket,
                                                  @NotNull TicketStatus expectedFromStatus,
                                                  @NotNull TicketStatusLog statusLog);

    /**
     * 查询活跃参与方
     *
     * @param ticketId           工单 ID
     * @param participantType    参与方类型
     * @param participantUserId  参与方用户 ID
     * @return 参与方实体
     */
    @NotNull
    Optional<TicketParticipant> findActiveParticipant(@NotNull Long ticketId,
                                                      @NotNull TicketParticipantType participantType,
                                                      @Nullable Long participantUserId);

    /**
     * 查询工单消息列表
     *
     * @param ticketId  工单 ID
     * @param beforeId  向前锚点
     * @param afterId   向后锚点
     * @param ascOrder  是否升序
     * @param size      返回条数
     * @return 消息列表
     */
    @NotNull
    List<TicketMessageView> listTicketMessages(@NotNull Long ticketId,
                                               @Nullable Long beforeId,
                                               @Nullable Long afterId,
                                               boolean ascOrder,
                                               int size);

    /**
     * 按消息主键查询消息实体
     *
     * @param ticketId   工单 ID
     * @param messageId  消息 ID
     * @return 消息实体
     */
    @NotNull
    Optional<TicketMessage> findTicketMessageById(@NotNull Long ticketId,
                                                  @NotNull Long messageId);

    /**
     * 按消息主键查询消息视图
     *
     * @param ticketId   工单 ID
     * @param messageId  消息 ID
     * @return 消息视图
     */
    @NotNull
    Optional<TicketMessageView> findTicketMessageViewById(@NotNull Long ticketId,
                                                          @NotNull Long messageId);

    /**
     * 按客户端消息幂等键查询消息视图
     *
     * @param ticketId         工单 ID
     * @param senderType       发送方类型
     * @param senderUserId     发送方用户 ID
     * @param clientMessageId  客户端消息幂等键
     * @return 消息视图
     */
    @NotNull
    Optional<TicketMessageView> findMessageViewByClientMessageId(@NotNull Long ticketId,
                                                                 @NotNull TicketParticipantType senderType,
                                                                 @Nullable Long senderUserId,
                                                                 @NotNull String clientMessageId);

    /**
     * 保存工单消息并更新工单最近消息时间
     *
     * @param userId   工单归属用户 ID
     * @param ticket   工单聚合
     * @param message  消息实体
     * @return 落库后的消息视图
     */
    @NotNull
    TicketMessageView saveTicketMessageAndTouchTicket(@NotNull Long userId,
                                                      @NotNull CustomerServiceTicket ticket,
                                                      @NotNull TicketMessage message);

    /**
     * 基于 CAS 条件更新消息内容
     *
     * @param ticketId   工单 ID
     * @param messageId  消息 ID
     * @param content    新内容
     * @param editedAt   编辑时间
     * @param updatedAt  更新时间
     * @return 更新是否成功
     */
    boolean updateTicketMessageContentWithCas(@NotNull Long ticketId,
                                              @NotNull Long messageId,
                                              @NotNull String content,
                                              @NotNull LocalDateTime editedAt,
                                              @NotNull LocalDateTime updatedAt);

    /**
     * 基于 CAS 条件撤回消息
     *
     * @param ticketId         工单 ID
     * @param messageId        消息 ID
     * @param recalledContent  撤回占位内容
     * @param recalledAt       撤回时间
     * @param updatedAt        更新时间
     * @return 更新是否成功
     */
    boolean recallTicketMessageWithCas(@NotNull Long ticketId,
                                       @NotNull Long messageId,
                                       @NotNull String recalledContent,
                                       @NotNull LocalDateTime recalledAt,
                                       @NotNull LocalDateTime updatedAt);

    /**
     * 校验消息 ID 是否属于工单
     *
     * @param ticketId   工单 ID
     * @param messageId  消息 ID
     * @return 是否属于当前工单
     */
    boolean existsMessageInTicket(@NotNull Long ticketId,
                                  @NotNull Long messageId);

    /**
     * 基于 CAS 条件更新参与方已读位点
     *
     * @param participantId      参与方 ID
     * @param ticketId           工单 ID
     * @param lastReadMessageId  最后已读消息 ID
     * @param lastReadAt         最后已读时间
     * @param updatedAt          更新时间
     * @return 更新是否成功
     */
    boolean updateParticipantReadWithCas(@NotNull Long participantId,
                                         @NotNull Long ticketId,
                                         @NotNull Long lastReadMessageId,
                                         @NotNull LocalDateTime lastReadAt,
                                         @NotNull LocalDateTime updatedAt);

    /**
     * 按工单号列表查询坐席可访问的工单 ID 列表
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketNos   工单号列表
     * @return 工单 ID 列表
     */
    @NotNull
    List<Long> listAgentTicketIdsByNos(@NotNull Long actorUserId,
                                       @NotNull List<String> ticketNos);

    /**
     * 按工单 ID 列表查询坐席可访问的工单 ID 列表
     *
     * @param actorUserId 操作者用户 ID
     * @param ticketIds   工单 ID 列表
     * @return 工单 ID 列表
     */
    @NotNull
    List<Long> listAgentTicketIdsByIds(@NotNull Long actorUserId,
                                       @NotNull List<Long> ticketIds);
}
