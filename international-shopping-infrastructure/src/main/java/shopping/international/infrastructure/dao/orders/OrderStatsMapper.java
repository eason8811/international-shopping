package shopping.international.infrastructure.dao.orders;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.orders.po.OrderStatsOverviewPO;
import shopping.international.infrastructure.dao.orders.po.OrderStatsRowPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper: 订单统计查询
 */
@Mapper
public interface OrderStatsMapper {

    /**
     * 查询统计概览
     *
     * @param from     时间起 (含)
     * @param to       时间止 (含)
     * @param status   状态过滤 (可为空)
     * @param currency 币种过滤 (可为空)
     * @return 概览聚合结果
     */
    OrderStatsOverviewPO selectOverview(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        @Param("status") String status,
                                        @Param("currency") String currency);

    /**
     * 查询按维度聚合的统计行
     *
     * @param from      时间起 (含)
     * @param to        时间止 (含)
     * @param dimension 维度枚举名
     * @param status    状态过滤 (可为空)
     * @param currency  币种过滤 (可为空)
     * @param top       Top N
     * @return 统计行列表
     */
    List<OrderStatsRowPO> selectStats(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("dimension") String dimension,
                                      @Param("status") String status,
                                      @Param("currency") String currency,
                                      @Param("top") int top);
}

