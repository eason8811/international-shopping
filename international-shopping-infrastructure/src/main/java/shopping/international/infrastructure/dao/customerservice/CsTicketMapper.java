package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketPO;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketQueryPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketShipmentSummaryPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper, cs_ticket 表
 */
@Mapper
public interface CsTicketMapper extends BaseMapper<CsTicketPO> {

    /**
     * 分页查询用户侧工单摘要
     *
     * @param userId       用户 ID
     * @param status       工单状态
     * @param issueType    问题类型
     * @param orderNo      订单号
     * @param shipmentNo   物流单号
     * @param createdFrom  创建时间起始
     * @param createdTo    创建时间结束
     * @param offset       分页偏移量
     * @param limit        分页条数
     * @return 工单摘要列表
     */
    List<CsTicketQueryPO> pageUserTicketSummaries(@Param("userId") Long userId,
                                                  @Param("status") String status,
                                                  @Param("issueType") String issueType,
                                                  @Param("orderNo") String orderNo,
                                                  @Param("shipmentNo") String shipmentNo,
                                                  @Param("createdFrom") LocalDateTime createdFrom,
                                                  @Param("createdTo") LocalDateTime createdTo,
                                                  @Param("offset") int offset,
                                                  @Param("limit") int limit);

    /**
     * 统计用户侧工单摘要总数
     *
     * @param userId       用户 ID
     * @param status       工单状态
     * @param issueType    问题类型
     * @param orderNo      订单号
     * @param shipmentNo   物流单号
     * @param createdFrom  创建时间起始
     * @param createdTo    创建时间结束
     * @return 总数
     */
    long countUserTicketSummaries(@Param("userId") Long userId,
                                  @Param("status") String status,
                                  @Param("issueType") String issueType,
                                  @Param("orderNo") String orderNo,
                                  @Param("shipmentNo") String shipmentNo,
                                  @Param("createdFrom") LocalDateTime createdFrom,
                                  @Param("createdTo") LocalDateTime createdTo);

    /**
     * 分页查询管理侧工单摘要
     *
     * @param ticketNo          工单编号
     * @param userId            用户 ID
     * @param orderId           订单 ID
     * @param shipmentId        物流单 ID
     * @param issueType         问题类型
     * @param status            工单状态
     * @param priority          工单优先级
     * @param assignedToUserId  指派坐席用户 ID
     * @param claimExternalId   理赔外部编号
     * @param slaDueFrom        SLA 到期时间起始
     * @param slaDueTo          SLA 到期时间结束
     * @param createdFrom       创建时间起始
     * @param createdTo         创建时间结束
     * @param offset            分页偏移量
     * @param limit             分页条数
     * @return 工单摘要列表
     */
    List<CsTicketQueryPO> pageAdminTicketSummaries(@Param("ticketNo") String ticketNo,
                                                   @Param("userId") Long userId,
                                                   @Param("orderId") Long orderId,
                                                   @Param("shipmentId") Long shipmentId,
                                                   @Param("issueType") String issueType,
                                                   @Param("status") String status,
                                                   @Param("priority") String priority,
                                                   @Param("assignedToUserId") Long assignedToUserId,
                                                   @Param("claimExternalId") String claimExternalId,
                                                   @Param("slaDueFrom") LocalDateTime slaDueFrom,
                                                   @Param("slaDueTo") LocalDateTime slaDueTo,
                                                   @Param("createdFrom") LocalDateTime createdFrom,
                                                   @Param("createdTo") LocalDateTime createdTo,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    /**
     * 统计管理侧工单摘要总数
     *
     * @param ticketNo          工单编号
     * @param userId            用户 ID
     * @param orderId           订单 ID
     * @param shipmentId        物流单 ID
     * @param issueType         问题类型
     * @param status            工单状态
     * @param priority          工单优先级
     * @param assignedToUserId  指派坐席用户 ID
     * @param claimExternalId   理赔外部编号
     * @param slaDueFrom        SLA 到期时间起始
     * @param slaDueTo          SLA 到期时间结束
     * @param createdFrom       创建时间起始
     * @param createdTo         创建时间结束
     * @return 总数
     */
    long countAdminTicketSummaries(@Param("ticketNo") String ticketNo,
                                   @Param("userId") Long userId,
                                   @Param("orderId") Long orderId,
                                   @Param("shipmentId") Long shipmentId,
                                   @Param("issueType") String issueType,
                                   @Param("status") String status,
                                   @Param("priority") String priority,
                                   @Param("assignedToUserId") Long assignedToUserId,
                                   @Param("claimExternalId") String claimExternalId,
                                   @Param("slaDueFrom") LocalDateTime slaDueFrom,
                                   @Param("slaDueTo") LocalDateTime slaDueTo,
                                   @Param("createdFrom") LocalDateTime createdFrom,
                                   @Param("createdTo") LocalDateTime createdTo);

    /**
     * 查询管理侧工单详情
     *
     * @param ticketId 工单 ID
     * @return 工单详情
     */
    CsTicketQueryPO selectAdminTicketDetailById(@Param("ticketId") Long ticketId);

    /**
     * 查询用户侧工单详情
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 工单详情
     */
    CsTicketQueryPO selectUserTicketDetail(@Param("userId") Long userId,
                                           @Param("ticketNo") String ticketNo);

    /**
     * 基于状态 CAS 更新工单状态
     *
     * @param ticketId             工单 ID
     * @param userId               用户 ID
     * @param expectedFromStatus   期望旧状态
     * @param toStatus             目标状态
     * @param resolvedAt           解决时间
     * @param closedAt             关闭时间
     * @param updatedAt            更新时间
     * @return 影响行数
     */
    int updateStatusWithCas(@Param("ticketId") Long ticketId,
                            @Param("userId") Long userId,
                            @Param("expectedFromStatus") String expectedFromStatus,
                            @Param("toStatus") String toStatus,
                            @Param("resolvedAt") LocalDateTime resolvedAt,
                            @Param("closedAt") LocalDateTime closedAt,
                            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 按状态和更新时间阈值分批扫描自动关单候选工单
     *
     * @param status 候选状态
     * @param updatedBefore 更新时间上限
     * @param limit 单批返回数量上限
     * @return 候选工单列表
     */
    List<CsTicketPO> listAutoCloseCandidatesByStatusAndUpdatedBefore(@Param("status") String status,
                                                                     @Param("updatedBefore") LocalDateTime updatedBefore,
                                                                     @Param("limit") int limit);

    /**
     * 更新工单最近消息时间, 使用单调更新避免并发覆盖
     *
     * @param ticketId     工单 ID
     * @param userId       用户 ID
     * @param messageTime  消息时间
     * @return 影响行数
     */
    int touchLastMessageAt(@Param("ticketId") Long ticketId,
                           @Param("userId") Long userId,
                           @Param("messageTime") LocalDateTime messageTime);

    /**
     * 查询工单关联补发单下的物流摘要列表
     *
     * @param ticketId 工单 ID
     * @return 物流摘要列表
     */
    List<CsUserTicketShipmentSummaryPO> listTicketReshipShipmentSummaries(@Param("ticketId") Long ticketId);
}
