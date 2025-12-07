package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SKU 规格绑定操作响应
 *
 * <p>返回受影响的 SKU 与规格列表, 用于规格绑定的新增或更新场景。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuSpecOperationRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * SKU ID
     */
    private Long skuId;
    /**
     * 受影响的规格 ID 列表
     */
    private List<Long> specIds;
}
