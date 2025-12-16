package shopping.international.api.resp.orders;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.PayChannel;
import shopping.international.domain.model.enums.orders.PayStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户侧订单详情响应 (OrderDetailRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailRespond {
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
     * 支付侧外部单号/交易号 (可为空)
     */
    private String paymentExternalId;
    /**
     * 支付时间 (可为空)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    /**
     * 收货信息快照
     */
    private AddressSnapshotRespond addressSnapshot;
    /**
     * 买家备注 (可为空)
     */
    private String buyerRemark;
    /**
     * 取消原因 (可为空)
     */
    private String cancelReason;
    /**
     * 取消时间 (可为空)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 明细列表
     */
    private List<OrderItemRespond> items;
    /**
     * 是否曾修改过地址
     */
    private Boolean addressChanged;
}

