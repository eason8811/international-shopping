package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 操作响应 SkuOperationRespond
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuOperationRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * SKU ID
     */
    private Long skuId;
}
