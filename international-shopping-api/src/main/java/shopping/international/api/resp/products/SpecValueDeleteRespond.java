package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格值删除响应 SpecValueDeleteRespond
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecValueDeleteRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * 规格 ID
     */
    private Long specId;
    /**
     * 规格值 ID
     */
    private Long valueId;
    /**
     * 是否删除
     */
    private Boolean deleted;
}
