package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 商品状态更新请求 (ProductStatusUpdateRequest)
 */
@Data
public class ProductStatusUpdateRequest implements Verifiable {
    /**
     * 目标商品状态
     */
    private ProductStatus status;

    /**
     * 校验状态字段
     *
     * @throws IllegalParamException 当状态为空时抛出
     */
    public void validate() {
        requireNotNull(status, "商品状态不能为空");
    }
}
