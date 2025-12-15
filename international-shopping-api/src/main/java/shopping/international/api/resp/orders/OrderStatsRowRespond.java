package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.OrderStatsDimension;

/**
 * 订单统计行响应 (OrderStatsRowRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsRowRespond {
    /**
     * 聚合维度
     */
    private OrderStatsDimension dimension;
    /**
     * 维度主键 (SPU/SKU/user_id/discount_code_id/policy_id)
     */
    private Long keyId;
    /**
     * 维度编码 (可选展示字段, 如折扣码 code)
     */
    private String keyCode;
    /**
     * 维度名称 (可选展示字段, 如折扣策略名)
     */
    private String keyName;
    /**
     * 订单数
     */
    private Integer ordersCount;
    /**
     * 件数 (可为空)
     */
    private Integer itemsCount;
    /**
     * 明细小计聚合 (可为空, 金额字符串)
     */
    private String subtotalAmountSum;
    /**
     * 实付金额聚合 (可为空, 金额字符串)
     */
    private String payAmountSum;
    /**
     * 折扣金额聚合 (可为空, 金额字符串)
     */
    private String discountAmountSum;
    /**
     * 运费金额聚合 (可为空, 金额字符串)
     */
    private String shippingAmountSum;
    /**
     * 折扣实际抵扣金额聚合 (可为空, 金额字符串)
     */
    private String appliedAmountSum;
}

