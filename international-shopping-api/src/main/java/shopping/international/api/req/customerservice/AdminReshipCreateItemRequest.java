package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧补发单明细创建请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReshipCreateItemRequest implements Verifiable {
    /**
     * 原订单明细 ID
     */
    @Nullable
    private Long orderItemId;
    /**
     * SKU ID
     */
    @Nullable
    private Long skuId;
    /**
     * 补发数量
     */
    @Nullable
    private Integer quantity;

    /**
     * 对补发单明细创建参数进行校验
     */
    @Override
    public void validate() {
        requireNotNull(orderItemId, "orderItemId 不能为空");
        require(orderItemId >= 1, "orderItemId 必须大于等于 1");

        requireNotNull(skuId, "skuId 不能为空");
        require(skuId >= 1, "skuId 必须大于等于 1");

        requireNotNull(quantity, "quantity 不能为空");
        require(quantity >= 1, "quantity 必须大于等于 1");
    }
}
