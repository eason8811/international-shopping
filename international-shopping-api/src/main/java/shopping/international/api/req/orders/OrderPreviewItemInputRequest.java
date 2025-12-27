package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 订单预览/创建条目输入 (OrderPreviewItemInputRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewItemInputRequest implements Verifiable {
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 数量 (最小为 1)
     */
    private Integer quantity;

    /**
     * 校验字段合法性
     */
    @Override
    public void validate() {
        requireNotNull(skuId, "skuId 不能为空");
        require(skuId >= 1, "skuId 必须大于等于 1");
        requireNotNull(quantity, "quantity 不能为空");
        require(quantity >= 1, "quantity 必须大于等于 1");
    }
}

