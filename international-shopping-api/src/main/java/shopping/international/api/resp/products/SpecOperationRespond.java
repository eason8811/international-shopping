package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 规格操作响应 SpecOperationRespond
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecOperationRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * 受影响的规格 ID 列表
     */
    private List<Long> specIds;
}
