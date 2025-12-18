package shopping.international.domain.service.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderStatsDimension;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.vo.orders.OrderStatsOverviewQuery;
import shopping.international.domain.model.vo.orders.OrderStatsQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理侧订单统计领域服务接口
 */
public interface IAdminStatsService {

    /**
     * 统计概览展示项
     *
     * @param ordersCount     订单数
     * @param paidOrdersCount 已支付订单数
     * @param itemsCount      商品件数合计
     * @param totalAmount     商品总额合计 (金额字符串)
     * @param discountAmount  折扣金额合计 (金额字符串)
     * @param shippingAmount  运费合计 (金额字符串)
     * @param payAmount       应付金额合计 (金额字符串)
     * @param currency        币种 (可为空, 若多币种混算)
     */
    record OrderStatsOverviewView(Long ordersCount,
                                  Long paidOrdersCount,
                                  Long itemsCount,
                                  String totalAmount,
                                  String discountAmount,
                                  String shippingAmount,
                                  String payAmount,
                                  @Nullable String currency) {
    }

    /**
     * 维度统计行展示项
     *
     * @param dimension      聚合维度
     * @param dimensionKey   维度键 (如 productId / skuId / userId / discountCodeId / policyId)
     * @param ordersCount    订单数
     * @param itemsCount     商品件数合计
     * @param totalAmount    商品总额合计 (金额字符串)
     * @param discountAmount 折扣金额合计 (金额字符串)
     * @param shippingAmount 运费合计 (金额字符串)
     * @param payAmount      应付金额合计 (金额字符串)
     * @param currency       币种 (可为空)
     */
    record OrderStatsRowView(@NotNull OrderStatsDimension dimension,
                             String dimensionKey,
                             Long ordersCount,
                             Long itemsCount,
                             String totalAmount,
                             String discountAmount,
                             String shippingAmount,
                             String payAmount,
                             @Nullable String currency) {
    }

    /**
     * 查询订单统计概览
     *
     * @param query 查询条件
     * @return 概览
     */
    @NotNull
    OrderStatsOverviewView overview(@NotNull OrderStatsOverviewQuery query);

    /**
     * 查询订单统计 (按维度聚合)
     *
     * @param query 查询条件
     * @return 统计行
     */
    @NotNull
    List<OrderStatsRowView> stats(@NotNull OrderStatsQuery query);
}
