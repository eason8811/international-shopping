package shopping.international.api.req.products;

import lombok.Data;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.StockAdjustMode;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 库存调整请求 StockAdjustRequest
 */
@Data
public class StockAdjustRequest implements Verifiable {
    /**
     * 调整模式, 支持设置/增加/减少
     */
    private StockAdjustMode mode;
    /**
     * 调整数量
     */
    private Integer quantity;
    /**
     * 调整原因备注
     */
    @Nullable
    private String reason;

    /**
     * 校验并规范化库存调整请求
     *
     * @throws IllegalParamException 当模式或数量非法时抛出 IllegalParamException
     */
    public void validate() {
        requireNotNull(mode, "库存调整模式不能为空");
        requireNotNull(quantity, "库存调整数量不能为空");
        if (mode == StockAdjustMode.SET) {
            if (quantity < 0)
                throw new IllegalParamException("库存设置数量不能为负数");
        } else if (quantity <= 0)
            throw new IllegalParamException("库存变更数量必须大于 0");

        reason = normalizeNullableField(reason, "库存调整原因不能为空", r -> r.length() <= 255, "库存调整原因长度不能超过 255 个字符");
    }
}
