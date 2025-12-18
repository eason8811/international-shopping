package shopping.international.domain.adapter.repository.orders;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.vo.orders.OrderStatsOverviewQuery;
import shopping.international.domain.model.vo.orders.OrderStatsQuery;
import shopping.international.domain.service.orders.IAdminStatsService;
import java.util.List;

/**
 * 订单统计查询仓储接口
 *
 * <p>职责:</p>
 * <ul>
 *     <li>提供管理侧统计概览 (数量/金额汇总)</li>
 *     <li>提供按维度聚合的统计结果 (SPU/SKU/USER/DISCOUNT_CODE/DISCOUNT_POLICY)</li>
 * </ul>
 */
public interface IOrderStatsRepository {
    /**
     * 查询统计概览
     *
     * @param query 查询条件
     * @return 统计概览
     */
    @NotNull
    IAdminStatsService.OrderStatsOverviewView overview(@NotNull OrderStatsOverviewQuery query);

    /**
     * 查询按维度聚合的统计行
     *
     * @param query 查询条件
     * @return 统计行列表
     */
    @NotNull
    List<IAdminStatsService.OrderStatsRowView> stats(@NotNull OrderStatsQuery query);
}
