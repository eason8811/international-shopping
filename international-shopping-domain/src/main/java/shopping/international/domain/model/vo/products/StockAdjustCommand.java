package shopping.international.domain.model.vo.products;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 库存调整命令 StockAdjustCommand
 *
 * <p>封装库存调整模式、数量与原因, 供领域服务统一处理库存变化</p>
 */
@Getter
@ToString
public class StockAdjustCommand {
    /**
     * 调整模式
     */
    private final StockAdjustMode mode;
    /**
     * 调整数量
     */
    private final int quantity;
    /**
     * 调整原因
     */
    @Nullable
    private final String reason;

    private StockAdjustCommand(StockAdjustMode mode, int quantity, @Nullable String reason) {
        this.mode = mode;
        this.quantity = quantity;
        this.reason = reason;
    }

    /**
     * 构建库存调整命令
     *
     * @param mode     调整模式
     * @param quantity 调整数量
     * @param reason   调整原因
     * @return 库存调整命令
     * @throws IllegalParamException 当模式为空或数量非法时抛出 IllegalParamException
     */
    public static StockAdjustCommand of(StockAdjustMode mode, int quantity, @Nullable String reason) {
        requireNotNull(mode, "库存调整模式不能为空");
        if (mode == StockAdjustMode.SET) {
            if (quantity < 0)
                throw new IllegalParamException("库存设置数量不能为负数");
        } else if (quantity <= 0)
            throw new IllegalParamException("库存变更数量必须大于 0");

        String normalizedReason = reason == null ? null : reason.strip();
        return new StockAdjustCommand(mode, quantity, normalizedReason);
    }
}
