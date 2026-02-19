package shopping.international.infrastructure.dao.shipping.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待补建物流单的订单候选行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaidOrderCandidatePO {

    /**
     * 订单主键
     */
    private Long orderId;
    /**
     * 订单号
     */
    private String orderNo;
}
