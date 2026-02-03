package shopping.international.domain.model.vo.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 确认退款计划项
 *
 * @param orderItemId 订单项 ID
 * @param quantity    数量
 * @param amountMinor 价格 Minor 模式
 * @param reason      理由
 */
public record ConfirmRefundPlanItem(@NotNull Long orderItemId,
                                    int quantity,
                                    long amountMinor,
                                    @Nullable String reason) implements Verifiable {

    /**
     * 验证当前退款计划项的有效性
     */
    @Override
    public void validate() {
        requireNotNull(orderItemId, "orderItemId 不能为空");
        require(quantity > 0, "quantity 必须大于 0");
        require(amountMinor > 0, "amountMinor 必须大于 0");
        if (reason != null)
            require(reason.length() <= 255, "reason 长度不能超过 255 个字符");
    }
}

