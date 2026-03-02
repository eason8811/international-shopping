package shopping.international.domain.adapter.repository.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.TicketAssignmentLog;
import shopping.international.domain.model.entity.customerservice.TicketParticipant;
import shopping.international.domain.model.entity.customerservice.TicketStatusLog;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminTicketDetailView;
import shopping.international.domain.model.vo.customerservice.AdminTicketPageCriteria;
import shopping.international.domain.model.vo.customerservice.AdminTicketSummaryView;

import java.time.LocalDateTime;
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
}
