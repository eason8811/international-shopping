package shopping.international.api.resp.products;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类操作响应 CategoryOperationRespond
 *
 * <p>用于返回分类更新、删除等操作的简要结果</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryOperationRespond {
    /**
     * 分类 ID
     */
    private Long categoryId;
    /**
     * 是否已删除
     */
    private Boolean deleted;
}
