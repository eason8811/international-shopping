package shopping.international.domain.model.vo.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 确认退款计划
 *
 * @param fullRefund          是否全额退款
 * @param refundAmountMinor   退款金额 Minor 模式
 * @param itemsAmountMinor    商品总额 Minor 模式
 * @param shippingAmountMinor 运费退款额 Minor 模式
 * @param items               退款计划项 {@link ConfirmRefundPlanItem}
 */
public record ConfirmRefundPlan(boolean fullRefund,
                                long refundAmountMinor,
                                @Nullable Long itemsAmountMinor,
                                @Nullable Long shippingAmountMinor,
                                @NotNull List<ConfirmRefundPlanItem> items) implements Verifiable {

    /**
     * 确认退款计划构造函数
     *
     * @param fullRefund          是否全额退款
     * @param refundAmountMinor   退款金额 Minor 模式
     * @param itemsAmountMinor    商品总额 Minor 模式
     * @param shippingAmountMinor 运费退款额 Minor 模式
     * @param items               退款计划项列表, 不可为空
     */
    public ConfirmRefundPlan {
        items = List.copyOf(items);
    }

    /**
     * 验证当前退款计划的有效性
     */
    @Override
    public void validate() {
        require(refundAmountMinor > 0, "refundAmountMinor 必须大于 0");
        if (itemsAmountMinor != null)
            require(itemsAmountMinor >= 0, "itemsAmountMinor 不能为负数");
        if (shippingAmountMinor != null)
            require(shippingAmountMinor >= 0, "shippingAmountMinor 不能为负数");
        requireNotNull(items, "items 不能为空");
        for (ConfirmRefundPlanItem item : items) {
            requireNotNull(item, "items 不能为空");
            item.validate();
        }
    }
}

