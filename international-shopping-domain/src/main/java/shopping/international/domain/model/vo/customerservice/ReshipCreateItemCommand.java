package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.NotNull;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 补发单明细创建命令
 *
 * @param orderItemId 原订单明细 ID
 * @param skuId       SKU ID
 * @param quantity    补发数量
 */
public record ReshipCreateItemCommand(@NotNull Long orderItemId,
                                      @NotNull Long skuId,
                                      @NotNull Integer quantity) implements Verifiable {

    /**
     * 校验补发明细创建命令
     */
    @Override
    public void validate() {
        require(orderItemId >= 1, "orderItemId 必须大于等于 1");
        require(skuId >= 1, "skuId 必须大于等于 1");
        require(quantity >= 1, "quantity 必须大于等于 1");
    }
}
