package shopping.international.domain.model.vo.orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.orders.DiscountApplyScope;
import shopping.international.domain.model.enums.orders.DiscountStrategyType;
import shopping.international.types.utils.Verifiable;

/**
 * 折扣策略筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class DiscountPolicySearchCriteria implements Verifiable {
    /**
     * 折扣策略名称
     */
    @Nullable
    private String name;
    /**
     * 作用域过滤 (整单/明细), 可空
     */
    @Nullable
    private DiscountApplyScope applyScope;
    /**
     * 策略类型过滤 (百分比/固定金额), 可空
     */
    @Nullable
    private DiscountStrategyType strategyType;

    /**
     * 校验筛选条件
     *
     * <p>当前仅为简单筛选, 不包含强校验逻辑</p>
     */
    @Override
    public void validate() {
        name = name == null || name.isBlank() ? null : name.strip();
    }
}
