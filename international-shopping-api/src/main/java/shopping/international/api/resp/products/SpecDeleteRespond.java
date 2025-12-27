package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规格删除响应 SpecDeleteRespond
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecDeleteRespond {
    /**
     * 商品 ID
     */
    private Long productId;
    /**
     * 规格 ID
     */
    private Long specId;
    /**
     * 是否删除
     */
    private Boolean deleted;
}
