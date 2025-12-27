package shopping.international.infrastructure.dao.orders.po;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 折扣实际使用流水视图 PO
 *
 * <p>用于联表查询 {@code order_discount_applied + orders} 的展示字段</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDiscountAppliedViewPO {
    /**
     * 主键 ID
     */
    private Long id;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单明细 ID (可为空)
     */
    private Long orderItemId;
    /**
     * 折扣码 ID
     */
    private Long discountCodeId;
    /**
     * 应用范围
     */
    private String appliedScope;
    /**
     * 抵扣金额
     */
    private BigDecimal appliedAmount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

