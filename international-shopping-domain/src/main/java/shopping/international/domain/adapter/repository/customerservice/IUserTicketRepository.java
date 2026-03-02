package shopping.international.domain.adapter.repository.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.TicketNo;
import shopping.international.domain.model.vo.customerservice.UserTicketCreateResult;
import shopping.international.domain.model.vo.customerservice.UserTicketDetailView;
import shopping.international.domain.model.vo.customerservice.UserTicketSummaryView;

import java.time.LocalDateTime;
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
}
