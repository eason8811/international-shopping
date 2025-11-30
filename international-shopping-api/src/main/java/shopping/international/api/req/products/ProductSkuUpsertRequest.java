package shopping.international.api.req.products;

import lombok.Data;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.ArrayList;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 批量维护 SKU 请求 ProductSkuUpsertRequest
 */
@Data
public class ProductSkuUpsertRequest {
    /**
     * SKU 列表
     */
    private List<ProductSkuUpsertRequestItem> skus;

    /**
     * 校验并规范化 SKU 列表
     *
     * @throws IllegalParamException 当列表为空或存在非法 SKU 条目时抛出 IllegalParamException
     */
    public void validate() {
        requireNotNull(skus, "SKU 列表不能为空");
        List<ProductSkuUpsertRequestItem> normalized = new ArrayList<>();
        for (ProductSkuUpsertRequestItem item : skus) {
            if (item == null)
                continue;
            item.validate();
            normalized.add(item);
        }
        if (normalized.isEmpty())
            throw new IllegalParamException("SKU 列表不能为空");
        skus = normalized;
    }
}
