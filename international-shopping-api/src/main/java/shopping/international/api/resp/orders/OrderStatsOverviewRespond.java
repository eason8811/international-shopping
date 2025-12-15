package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单统计总览响应 (OrderStatsOverviewRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsOverviewRespond {
    /**
     * 统计开始时间
     */
    private LocalDateTime from;
    /**
     * 统计结束时间
     */
    private LocalDateTime to;
    /**
     * 币种 (可为空)
     */
    private String currency;
    /**
     * 订单数量
     */
    private Integer ordersCount;
    /**
     * 已支付订单数量
     */
    private Integer paidOrdersCount;
    /**
     * 明细件数合计 (可为空)
     */
    private Integer itemsCountSum;
    /**
     * 订单总金额合计 (金额字符串)
     */
    private String totalAmountSum;
    /**
     * 折扣金额合计 (金额字符串)
     */
    private String discountAmountSum;
    /**
     * 运费金额合计 (金额字符串)
     */
    private String shippingAmountSum;
    /**
     * 实付金额合计 (金额字符串)
     */
    private String payAmountSum;
}

