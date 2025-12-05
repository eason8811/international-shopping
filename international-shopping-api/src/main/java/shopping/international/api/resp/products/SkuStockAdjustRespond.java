package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SKU 库存调整响应 SkuStockAdjustRespond
 *
 * <p>返回调整后的库存与标识信息</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuStockAdjustRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 调整后的库存
     */
    private Integer stock;
}
