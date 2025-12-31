package shopping.international.infrastructure.adapter.repository.orders;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.orders.IOrderStatsRepository;
import shopping.international.domain.model.vo.orders.OrderStatsOverviewQuery;
import shopping.international.domain.model.vo.orders.OrderStatsQuery;
import shopping.international.domain.service.orders.IAdminStatsService;
import shopping.international.infrastructure.dao.orders.OrderStatsMapper;
import shopping.international.infrastructure.dao.orders.po.OrderStatsOverviewPO;
import shopping.international.infrastructure.dao.orders.po.OrderStatsRowPO;

import java.util.List;

/**
 * 基于 MyBatis 的订单统计仓储实现
 */
@Repository
@RequiredArgsConstructor
public class OrderStatsRepository implements IOrderStatsRepository {

    /**
     * 统计查询 Mapper
     */
    private final OrderStatsMapper orderStatsMapper;

    /**
     * 查询统计概览
     *
     * @param query 查询条件
     * @return 概览
     */
    @Override
    public @NotNull IAdminStatsService.OrderStatsOverviewView overview(@NotNull OrderStatsOverviewQuery query) {
        query.validate();
        String currency = query.getCurrency();
        OrderStatsOverviewPO po = orderStatsMapper.selectOverview(
                query.getFrom(),
                query.getTo(),
                query.getStatus() == null ? null : query.getStatus().name(),
                currency
        );
        if (po == null)
            return new IAdminStatsService.OrderStatsOverviewView(0L, 0L, 0L, 0L, 0L, 0L, 0L, currency);
        return new IAdminStatsService.OrderStatsOverviewView(
                po.getOrdersCount() == null ? 0L : po.getOrdersCount(),
                po.getPaidOrdersCount() == null ? 0L : po.getPaidOrdersCount(),
                po.getItemsCount() == null ? 0L : po.getItemsCount(),
                po.getTotalAmount(),
                po.getDiscountAmount(),
                po.getShippingAmount(),
                po.getPayAmount(),
                currency
        );
    }

    /**
     * 查询按维度聚合的统计行
     *
     * @param query 查询条件
     * @return 统计行
     */
    @Override
    public @NotNull List<IAdminStatsService.OrderStatsRowView> stats(@NotNull OrderStatsQuery query) {
        query.validate();
        int top = query.getTop() == null ? 100 : Math.min(Math.max(query.getTop(), 1), 1000);
        String currency = query.getCurrency();
        List<OrderStatsRowPO> pos = orderStatsMapper.selectStats(
                query.getFrom(),
                query.getTo(),
                query.getDimension().name(),
                query.getStatus() == null ? null : query.getStatus().name(),
                currency,
                top
        );
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(po -> new IAdminStatsService.OrderStatsRowView(
                query.getDimension(),
                po.getDimensionKey(),
                po.getOrdersCount() == null ? 0L : po.getOrdersCount(),
                po.getItemsCount() == null ? 0L : po.getItemsCount(),
                po.getTotalAmount(),
                po.getDiscountAmount(),
                po.getShippingAmount(),
                po.getPayAmount(),
                currency
        )).toList();
    }
}
