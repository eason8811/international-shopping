package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.customerservice.po.CsTicketPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketDetailPO;
import shopping.international.infrastructure.dao.customerservice.po.CsUserTicketSummaryPO;

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
    List<CsUserTicketSummaryPO> pageUserTicketSummaries(@Param("userId") Long userId,
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
     * 查询用户侧工单详情
     *
     * @param userId    用户 ID
     * @param ticketNo  工单编号
     * @return 工单详情
     */
    CsUserTicketDetailPO selectUserTicketDetail(@Param("userId") Long userId,
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
}
