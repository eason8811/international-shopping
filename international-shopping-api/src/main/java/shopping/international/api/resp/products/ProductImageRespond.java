package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品图片响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRespond {
    /**
     * 图片 URL
     */
    private String url;
    /**
     * 是否主图
     */
    private Boolean isMain;
    /**
     * 图片排序
     */
    private Integer sortOrder;
}
