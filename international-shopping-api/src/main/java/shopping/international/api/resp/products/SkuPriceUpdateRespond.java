package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SKU 价格更新响应 SkuPriceUpdateRespond
 *
 * <p>返回已更新价格的 SKU 标识与受影响的币种列表</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuPriceUpdateRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 已更新的币种列表
     */
    private List<String> currencies;
}
