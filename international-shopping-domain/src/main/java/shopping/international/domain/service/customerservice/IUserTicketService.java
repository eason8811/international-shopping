package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketIssueType;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.*;
import shopping.international.domain.model.vo.customerservice.TicketNo;

import java.time.LocalDateTime;

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
}
