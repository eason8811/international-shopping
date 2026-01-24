package shopping.international.domain.model.vo.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.payment.PaymentChannel;
import shopping.international.domain.model.enums.payment.PaymentStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧支付单筛选条件
 *
 * <p>用于后台列表/排障/对账等读模型查询</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentSearchCriteria implements Verifiable {

    /**
     * 按订单号筛选 (联表 orders.order_no)
     */
    @Nullable
    private String orderNo;

    /**
     * 按外部单号筛选 (payment_order.external_id)
     */
    @Nullable
    private String externalId;

    /**
     * 支付通道筛选
     */
    @Nullable
    private PaymentChannel channel;

    /**
     * 支付单状态筛选
     */
    @Nullable
    private PaymentStatus status;

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

