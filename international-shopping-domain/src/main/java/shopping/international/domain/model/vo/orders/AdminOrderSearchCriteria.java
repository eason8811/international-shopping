package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.domain.model.enums.orders.PayChannel;
import shopping.international.domain.model.enums.orders.PayStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧订单筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class AdminOrderSearchCriteria implements Verifiable {
    /**
     * 订单号 (可空)
     */
    @Nullable
    private String orderNo;
    /**
     * 用户ID (可空)
     */
    @Nullable
    private Long userId;
    /**
     * 订单状态 (可空)
     */
    @Nullable
    private OrderStatus status;
    /**
     * 支付状态 (可空)
     */
    @Nullable
    private PayStatus payStatus;
    /**
     * 支付通道 (可空)
     */
    @Nullable
    private PayChannel payChannel;
    /**
     * 支付网关 externalId (可空)
     */
    @Nullable
    private String paymentExternalId;
    /**
     * 创建时间起 (可空, 含)
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 创建时间止 (可空, 含)
     */
    @Nullable
    private LocalDateTime createdTo;
    /**
     * 币种过滤 (可空)
     */
    @Nullable
    private String currency;

    /**
     * 规范化与校验筛选条件
     *
     * <ul>
     *     <li>去除 {@code orderNo/paymentExternalId} 首尾空白, 空字符串转为 {@code null}</li>
     *     <li>若提供 {@code currency}, 则规范化为 3 位大写币种</li>
     *     <li>若同时提供 {@code createdFrom/createdTo}, 则保证起止时间不反转</li>
     * </ul>
     */
    @Override
    public void validate() {
        if (orderNo != null) {
            String trimmed = orderNo.strip();
            this.orderNo = trimmed.isEmpty() ? null : trimmed;
        }
        if (paymentExternalId != null) {
            String trimmed = paymentExternalId.strip();
            this.paymentExternalId = trimmed.isEmpty() ? null : trimmed;
        }
        if (currency != null)
            this.currency = normalizeCurrency(currency);
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");
    }
}
