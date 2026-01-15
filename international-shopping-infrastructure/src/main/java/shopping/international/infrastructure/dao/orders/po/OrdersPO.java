package shopping.international.infrastructure.dao.orders.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: orders
 *
 * <p>订单主表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt"})
@TableName("orders")
public class OrdersPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单号
     */
    @TableField("order_no")
    private String orderNo;
    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;
    /**
     * 订单状态
     */
    @TableField("status")
    private String status;
    /**
     * 商品件数
     */
    @TableField("items_count")
    private Integer itemsCount;
    /**
     * 商品总额
     */
    @TableField("total_amount")
    private Long totalAmount;
    /**
     * 折扣金额
     */
    @TableField("discount_amount")
    private Long discountAmount;
    /**
     * 运费
     */
    @TableField("shipping_amount")
    private Long shippingAmount;
    /**
     * 税费
     */
    @TableField("tax_amount")
    private Long taxAmount;
    /**
     * 应付金额
     */
    @TableField("pay_amount")
    private Long payAmount;
    /**
     * 币种
     */
    @TableField("currency")
    private String currency;
    /**
     * 支付通道
     */
    @TableField("pay_channel")
    private String payChannel;
    /**
     * 支付状态
     */
    @TableField("pay_status")
    private String payStatus;
    /**
     * 支付外部单号
     */
    @TableField("payment_external_id")
    private String paymentExternalId;
    /**
     * 支付时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;
    /**
     * 地址快照 JSON
     */
    @TableField("address_snapshot")
    private String addressSnapshot;
    /**
     * 买家备注
     */
    @TableField("buyer_remark")
    private String buyerRemark;
    /**
     * 取消原因
     */
    @TableField("cancel_reason")
    private String cancelReason;
    /**
     * 取消时间
     */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;
    /**
     * 退款原因快照 JSON
     */
    @TableField("refund_reason_snapshot")
    private String refundReasonSnapshot;
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
}
