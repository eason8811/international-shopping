package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.OrderStatsDimension;
import shopping.international.domain.model.enums.orders.OrderStatus;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 订单统计查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class OrderStatsQuery implements Verifiable {
    /**
     * 统计起始时间 (必填, 含)
     */
    @NotNull
    private LocalDateTime from;
    /**
     * 统计结束时间 (必填, 含)
     */
    @NotNull
    private LocalDateTime to;
    /**
     * 统计维度 (必填)
     */
    @NotNull
    private OrderStatsDimension dimension;
    /**
     * 订单状态过滤 (可空)
     */
    @Nullable
    private OrderStatus status;
    /**
     * 币种过滤 (可空; 建议传入避免多币种混算)
     */
    @Nullable
    private String currency;
    /**
     * Top N 限制 (可空; 1-1000)
     */
    private Integer top;

    /**
     * 规范化与校验统计查询参数
     *
     * <ul>
     *     <li>{@code from/to/dimension} 必填</li>
     *     <li>保证 {@code from <= to}</li>
     *     <li>若提供 {@code currency}, 则规范化为 3 位大写币种</li>
     *     <li>若提供 {@code top}, 则限制在 1-1000</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(from, "from 不能为空");
        requireNotNull(to, "to 不能为空");
        requireNotNull(dimension, "dimension 不能为空");
        require(!from.isAfter(to), "from 不能晚于 to");
        if (currency != null)
            this.currency = normalizeCurrency(currency);
        if (top != null) {
            require(top >= 1 && top <= 1000, "top 需在 1-1000 之间");
        }
    }
}
