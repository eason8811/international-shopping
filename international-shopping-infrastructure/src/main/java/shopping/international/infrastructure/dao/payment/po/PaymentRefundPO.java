package shopping.international.infrastructure.dao.payment.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 持久化对象: payment_refund
 *
 * <p>退款单主表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt", "items"})
@TableName("payment_refund")
public class PaymentRefundPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 退款单号 (对外可见)
     */
    @TableField("refund_no")
    private String refundNo;

    /**
     * 订单 ID (orders.id)
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 原支付单 ID (payment_order.id)
     */
    @TableField("payment_order_id")
    private Long paymentOrderId;

    /**
     * 网关退款 externalId (可为空)
     */
    @TableField("external_refund_id")
    private String externalRefundId;

    /**
     * 商户侧/客户端幂等键 (可为空)
     */
    @TableField("client_refund_no")
    private String clientRefundNo;

    /**
     * 退款总金额 (最小货币单位)
     */
    @TableField("amount")
    private Long amount;

    /**
     * 币种
     */
    @TableField("currency")
    private String currency;

    /**
     * 货品部分退款金额 (可为空)
     */
    @TableField("items_amount")
    private Long itemsAmount;

    /**
     * 运费部分退款金额 (可为空)
     */
    @TableField("shipping_amount")
    private Long shippingAmount;

    /**
     * 退款状态 (枚举字符串)
     */
    @TableField("status")
    private String status;

    /**
     * 原因分类 (枚举字符串)
     */
    @TableField("reason_code")
    private String reasonCode;

    /**
     * 原因备注 (可为空)
     */
    @TableField("reason_text")
    private String reasonText;

    /**
     * 发起方 (枚举字符串)
     */
    @TableField("initiator")
    private String initiator;

    /**
     * 关联工单 ID (可为空)
     */
    @TableField("ticket_id")
    private Long ticketId;

    /**
     * 退款请求报文 (JSON 字符串，可为空)
     */
    @TableField("request_payload")
    private String requestPayload;

    /**
     * 退款响应报文 (JSON 字符串，可为空)
     */
    @TableField("response_payload")
    private String responsePayload;

    /**
     * 最近一次回调报文 (JSON 字符串，可为空)
     */
    @TableField("notify_payload")
    private String notifyPayload;

    /**
     * 最近轮询时间
     */
    @TableField("last_polled_at")
    private LocalDateTime lastPolledAt;

    /**
     * 最近回调处理时间
     */
    @TableField("last_notified_at")
    private LocalDateTime lastNotifiedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    /**
     * 业务订单号 (联表字段，不落库)
     */
    @TableField(exist = false)
    private String orderNo;

    /**
     * 支付通道 (联表字段，不落库)
     */
    @TableField(exist = false)
    private String channel;

    /**
     * 退款明细列表 (联表字段，不落库)
     */
    @TableField(exist = false)
    private List<PaymentRefundItemPO> items;
}

