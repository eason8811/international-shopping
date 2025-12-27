package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 折扣实际使用流水筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDiscountAppliedSearchCriteria implements Verifiable {
    /**
     * 订单号过滤 (可空)
     */
    @Nullable
    private String orderNo;
    /**
     * 折扣码 ID 过滤 (可空)
     */
    @Nullable
    private Long discountCodeId;
    /**
     * 应用范围过滤 (可空)
     */
    @Nullable
    private DiscountApplyScope appliedScope;
    /**
     * 时间起 (可空, 含)
     */
    @Nullable
    private LocalDateTime from;
    /**
     * 时间止 (可空, 含)
     */
    @Nullable
    private LocalDateTime to;

    /**
     * 规范化与校验筛选条件
     *
     * <ul>
     *     <li>{@code orderNo} 去除首尾空白, 空字符串转为 {@code null}</li>
     *     <li>若同时提供 {@code from/to}, 则保证区间不反转</li>
     * </ul>
     */
    @Override
    public void validate() {
        if (orderNo != null) {
            String trimmed = orderNo.strip();
            this.orderNo = trimmed.isEmpty() ? null : trimmed;
        }
        if (from != null && to != null)
            require(!from.isAfter(to), "from 不能晚于 to");
    }
}

