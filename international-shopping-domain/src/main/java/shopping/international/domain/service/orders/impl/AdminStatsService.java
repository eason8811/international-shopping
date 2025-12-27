package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.orders.IOrderStatsRepository;
import shopping.international.domain.model.vo.orders.OrderStatsOverviewQuery;
import shopping.international.domain.model.vo.orders.OrderStatsQuery;
import shopping.international.domain.service.orders.IAdminStatsService;

import java.util.List;

/**
 * 管理侧订单统计领域服务默认实现
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService implements IAdminStatsService {

    /**
     * 统计查询仓储
     */
    private final IOrderStatsRepository orderStatsRepository;

    /**
     * 查询订单统计概览
     *
     * @param query 查询条件
     * @return 概览
     */
    @Override
    public @NotNull OrderStatsOverviewView overview(@NotNull OrderStatsOverviewQuery query) {
        return orderStatsRepository.overview(query);
    }

    /**
     * 查询订单统计 (按维度聚合)
     *
     * @param query 查询条件
     * @return 统计行
     */
    @Override
    public @NotNull List<OrderStatsRowView> stats(@NotNull OrderStatsQuery query) {
        return orderStatsRepository.stats(query);
    }
}
