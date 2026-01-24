package shopping.international.infrastructure.dao.payment.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象: payment_refund_item
 *
 * <p>退款明细表</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("payment_refund_item")
public class PaymentRefundItemPO {

    /**
     * 主键 ID (自增)
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 退款单 ID (payment_refund.id)
     */
    @TableField("refund_id")
    private Long refundId;

    /**
     * 订单明细 ID (order_item.id)
     */
    @TableField("order_item_id")
    private Long orderItemId;

    /**
     * 本次退款数量 (件)
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 退款金额 (最小货币单位)
     */
    @TableField("amount")
    private Long amount;

    /**
     * 明细退款原因备注 (可为空)
     */
    @TableField("reason")
    private String reason;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}

