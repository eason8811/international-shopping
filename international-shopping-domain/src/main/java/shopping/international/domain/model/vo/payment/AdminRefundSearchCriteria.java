package shopping.international.domain.model.vo.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.RefundStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧退款单筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundSearchCriteria implements Verifiable {

    /**
     * 按订单号筛选 (联表 orders.order_no)
     */
    @Nullable
    private String orderNo;

    /**
     * 按外部退款单号筛选 (payment_refund.external_refund_id)
     */
    @Nullable
    private String externalId;

    /**
     * 支付通道筛选
     */
    @Nullable
    private PaymentChannel channel;

    /**
     * 退款状态筛选
     */
    @Nullable
    private RefundStatus status;

    /**
     * 创建时间起 (含)
     */
    @Nullable
    private LocalDateTime createdFrom;

    /**
     * 创建时间止 (含)
     */
    @Nullable
    private LocalDateTime createdTo;

    /**
     * 规范化与校验筛选条件
     */
    @Override
    public void validate() {
        if (orderNo != null) {
            String trimmed = orderNo.strip();
            this.orderNo = trimmed.isEmpty() ? null : trimmed;
        }
        if (externalId != null) {
            String trimmed = externalId.strip();
            this.externalId = trimmed.isEmpty() ? null : trimmed;
        }
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");
    }
}

