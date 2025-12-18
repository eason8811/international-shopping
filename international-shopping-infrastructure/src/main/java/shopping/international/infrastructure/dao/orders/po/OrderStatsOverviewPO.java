package shopping.international.infrastructure.dao.orders.po;

import lombok.*;

import java.math.BigDecimal;

/**
 * 订单统计概览 PO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsOverviewPO {
    /**
     * 订单数
     */
    private Long ordersCount;
    /**
     * 已支付订单数 (pay_status=SUCCESS)
     */
    private Long paidOrdersCount;
    /**
     * 商品件数合计
     */
    private Long itemsCount;
    /**
     * 商品总额合计
     */
    private BigDecimal totalAmount;
    /**
     * 折扣金额合计
     */
    private BigDecimal discountAmount;
    /**
     * 运费合计
     */
    private BigDecimal shippingAmount;
    /**
     * 应付金额合计
     */
    private BigDecimal payAmount;
}
