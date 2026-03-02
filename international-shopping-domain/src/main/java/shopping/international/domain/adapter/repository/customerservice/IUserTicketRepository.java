package shopping.international.domain.adapter.repository.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketMessage;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.TicketMessageNo;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketMessageView;
import shopping.international.domain.model.vo.customerservice.UserTicketShipmentSummaryView;
import shopping.international.domain.model.vo.customerservice.UserTicketStatusLogView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户侧工单仓储接口
 */
public interface IUserTicketRepository {

    /**
     * 分页查询用户工单摘要
     *
     * @param userId       用户 ID
     * @param pageQuery    分页参数
     * @param status       状态筛选
     * @param issueType    问题类型筛选
     * @param orderNo      订单号筛选
     * @param shipmentNo   物流单号筛选
     * @param createdFrom  创建时间起始
     * @param createdTo    创建时间结束
     * @return 工单摘要分页结果
     */
    @NotNull
    PageResult<UserTicketSummaryView> pageUserTicketSummaries(@NotNull Long userId,
                                                              @NotNull PageQuery pageQuery,
                                                              @Nullable TicketStatus status,
                                                              @Nullable TicketIssueType issueType,
                                                              @Nullable String orderNo,
                                                              @Nullable String shipmentNo,
                                                              @Nullable LocalDateTime createdFrom,
                                                              @Nullable LocalDateTime createdTo);

    /**
     * 按用户和工单编号查询工单详情视图
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 工单详情视图
     */
    @NotNull
    Optional<UserTicketDetailView> findUserTicketDetail(@NotNull Long userId,
                                                        @NotNull TicketNo ticketNo);

    /**
     * 按用户和工单编号查询工单聚合
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 工单聚合
     */
    @NotNull
    Optional<CustomerServiceTicket> findByUserAndTicketNo(@NotNull Long userId,
                                                          @NotNull TicketNo ticketNo);

    /**
     * 按去重维度查询进行中的工单
     *
     * @param userId      用户 ID
     * @param orderId     订单 ID
     * @param shipmentId  物流单 ID
     * @param issueType   问题类型
     * @return 进行中的工单
     */
    @NotNull
    Optional<CustomerServiceTicket> findOpenTicketByDedupe(@NotNull Long userId,
                                                           @NotNull Long orderId,
                                                           @Nullable Long shipmentId,
                                                           @NotNull TicketIssueType issueType);

    /**
     * 保存新建工单, 同时写入默认参与方和初始化状态日志
     *
     * @param ticket        待保存工单聚合
     * @param ownerUserId   发起用户 ID
     * @param sourceRef     来源引用标识
     * @return 创建结果
     */
    @NotNull
    UserTicketCreateResult saveNewTicket(@NotNull CustomerServiceTicket ticket,
                                         @NotNull Long ownerUserId,
                                         @Nullable String sourceRef);

    /**
     * 按用户和工单编号查询创建结果
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 创建结果
     */
    @NotNull
    Optional<UserTicketCreateResult> findUserTicketCreateResult(@NotNull Long userId,
                                                                @NotNull TicketNo ticketNo);

    /**
     * 基于状态 CAS 更新工单状态, 并写入状态日志
     *
     * @param userId               用户 ID
     * @param ticket               已完成状态推进的工单聚合
     * @param expectedFromStatus   期望旧状态
     * @param statusLog            状态日志实体
     * @return 更新是否成功
     */
    boolean updateTicketStatusWithCasAndAppendLog(@NotNull Long userId,
                                                  @NotNull CustomerServiceTicket ticket,
                                                  @NotNull TicketStatus expectedFromStatus,
                                                  @NotNull TicketStatusLog statusLog);

    /**
     * 查询工单消息列表
     *
     * @param ticketId   工单 ID
     * @param beforeId   向前锚点
     * @param afterId    向后锚点
     * @param ascOrder   是否升序
     * @param size       返回条数
     * @return 消息列表
     */
    @NotNull
    List<UserTicketMessageView> listTicketMessages(@NotNull Long ticketId,
                                                   @Nullable Long beforeId,
                                                   @Nullable Long afterId,
                                                   boolean ascOrder,
                                                   int size);

    /**
     * 按消息编号查询消息实体
     *
     * @param ticketId    工单 ID
     * @param messageNo   消息编号
     * @return 消息实体
     */
    @NotNull
    Optional<TicketMessage> findTicketMessageByNo(@NotNull Long ticketId,
                                                  @NotNull TicketMessageNo messageNo);

    /**
     * 按消息编号查询消息视图
     *
     * @param ticketId    工单 ID
     * @param messageNo   消息编号
     * @return 消息视图
     */
    @NotNull
    Optional<UserTicketMessageView> findTicketMessageViewByNo(@NotNull Long ticketId,
                                                              @NotNull TicketMessageNo messageNo);

    /**
     * 按客户端消息幂等键查询消息视图
     *
     * @param ticketId          工单 ID
     * @param senderType        发送方类型
     * @param senderUserId      发送方用户 ID
     * @param clientMessageId   客户端消息幂等键
     * @return 消息视图
     */
    @NotNull
    Optional<UserTicketMessageView> findMessageViewByClientMessageId(@NotNull Long ticketId,
                                                                     @NotNull TicketParticipantType senderType,
                                                                     @Nullable Long senderUserId,
                                                                     @NotNull String clientMessageId);

    /**
     * 保存工单消息并更新工单最近消息时间
     *
     * @param userId    用户 ID
     * @param ticket    工单聚合
     * @param message   消息实体
     * @return 落库后的消息视图
     */
    @NotNull
    UserTicketMessageView saveTicketMessageAndTouchTicket(@NotNull Long userId,
                                                          @NotNull CustomerServiceTicket ticket,
                                                          @NotNull TicketMessage message);

    /**
     * 基于 CAS 条件更新消息内容
     *
     * @param ticketId    工单 ID
     * @param messageId   消息 ID
     * @param content     新内容
     * @param editedAt    编辑时间
     * @param updatedAt   更新时间
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
     * @param ticketId             工单 ID
     * @param messageId            消息 ID
     * @param recalledContent      撤回占位内容
     * @param recalledAt           撤回时间
     * @param updatedAt            更新时间
     * @return 更新是否成功
     */
    boolean recallTicketMessageWithCas(@NotNull Long ticketId,
                                       @NotNull Long messageId,
                                       @NotNull String recalledContent,
                                       @NotNull LocalDateTime recalledAt,
                                       @NotNull LocalDateTime updatedAt);

    /**
     * 查询活跃参与方
     *
     * @param ticketId          工单 ID
     * @param participantType   参与方类型
     * @param participantUserId 参与方用户 ID
     * @return 参与方实体
     */
    @NotNull
    Optional<TicketParticipant> findActiveParticipant(@NotNull Long ticketId,
                                                      @NotNull TicketParticipantType participantType,
                                                      @Nullable Long participantUserId);

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
     * 分页查询工单状态日志
     *
     * @param ticketId    工单 ID
     * @param pageQuery   分页参数
     * @return 状态日志分页结果
     */
    @NotNull
    PageResult<UserTicketStatusLogView> pageTicketStatusLogs(@NotNull Long ticketId,
                                                             @NotNull PageQuery pageQuery);

    /**
     * 查询工单关联补发物流摘要
     *
     * @param ticketId 工单 ID
     * @return 物流摘要列表
     */
    @NotNull
    List<UserTicketShipmentSummaryView> listTicketReshipShipments(@NotNull Long ticketId);

    /**
     * 按工单号列表查询属于用户的工单 ID 列表
     *
     * @param userId      用户 ID
     * @param ticketNos   工单号列表
     * @return 工单 ID 列表
     */
    @NotNull
    List<Long> listOwnedTicketIdsByNos(@NotNull Long userId,
                                       @NotNull List<String> ticketNos);

    /**
     * 按工单 ID 列表查询属于用户的工单 ID 列表
     *
     * @param userId     用户 ID
     * @param ticketIds  工单 ID 列表
     * @return 工单 ID 列表
     */
    @NotNull
    List<Long> listOwnedTicketIdsByIds(@NotNull Long userId,
                                       @NotNull List<Long> ticketIds);
}
