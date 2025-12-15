package shopping.international.api.resp.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.PayChannel;
import shopping.international.domain.model.enums.orders.PayStatus;

import java.time.LocalDateTime;

/**
 * 用户侧订单列表摘要响应 (OrderSummaryRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryRespond {
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 订单状态
     */
    private OrderStatus status;
    /**
     * 购买件数
     */
    private Integer itemsCount;
    /**
     * 订单总金额 (可为空, 金额字符串)
     */
    private String totalAmount;
    /**
     * 折扣金额 (可为空, 金额字符串)
     */
    private String discountAmount;
    /**
     * 运费金额 (可为空, 金额字符串)
     */
    private String shippingAmount;
    /**
     * 实付金额 (金额字符串)
     */
    private String payAmount;
    /**
     * 币种
     */
    private String currency;
    /**
     * 支付渠道
     */
    private PayChannel payChannel;
    /**
     * 支付状态
     */
    private PayStatus payStatus;
    /**
     * 支付时间 (可为空)
     */
    private LocalDateTime payTime;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

