package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧手动建单商品明细请求对象 (AdminManualShipmentItemInputRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminManualShipmentItemInputRequest implements Verifiable {
    /**
     * 订单明细 ID
     */
    @Nullable
    private Long orderItemId;
    /**
     * 商品 SPU ID
     */
    @Nullable
    private Long productId;
    /**
     * 商品 SKU ID
     */
    @Nullable
    private Long skuId;
    /**
     * 本物流单内的发货数量
     */
    @Nullable
    private Integer quantity;

    /**
     * 对手动建单商品明细进行校验
     */
    @Override
    public void validate() {
        requireNotNull(orderItemId, "orderItemId 不能为空");
        require(orderItemId >= 1, "orderItemId 必须大于等于 1");

        if (productId != null)
            require(productId >= 1, "productId 必须大于等于 1");
        if (skuId != null)
            require(skuId >= 1, "skuId 必须大于等于 1");

        requireNotNull(quantity, "quantity 不能为空");
        require(quantity >= 1, "quantity 必须大于等于 1");
    }
}
