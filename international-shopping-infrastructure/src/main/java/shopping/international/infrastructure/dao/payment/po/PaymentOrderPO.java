package shopping.international.infrastructure.dao.payment.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: payment_order
 *
 * <p>支付单主表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("payment_order")
public class PaymentOrderPO {
    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单 ID (orders.id)
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 支付网关 externalId (可为空)
     */
    @TableField("external_id")
    private String externalId;
    /**
     * capture ID (用于退款, 目前仅 PayPal 需要)
     */
    @TableField("capture_id")
    private String capture_id;
    /**
     * 支付通道 (枚举字符串)
     */
    @TableField("channel")
    private String channel;
    /**
     * 支付金额 (最小货币单位)
     */
    @TableField("amount")
    private Long amount;
    /**
     * 币种
     */
    @TableField("currency")
    private String currency;
    /**
     * 支付单状态 (枚举字符串)
     */
    @TableField("status")
    private String status;
    /**
     * 下单请求报文 (JSON 字符串，可为空)
     */
    @TableField("request_payload")
    private String requestPayload;
    /**
     * 下单响应报文 (JSON 字符串，可为空)
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
}

