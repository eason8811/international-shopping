package shopping.international.infrastructure.dao.orders.po;

import lombok.*;

import java.math.BigDecimal;

/**
 * 订单维度统计行 PO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsRowPO {
    /**
     * 维度键
     */
    private String dimensionKey;
    /**
     * 订单数
     */
    private Long ordersCount;
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
