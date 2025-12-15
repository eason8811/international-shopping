package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.utils.Verifiable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 折扣码适用商品覆盖设置请求体 (DiscountCodeProductUpsertRequest)
 *
 * <p>用于覆盖写入折扣码与商品的映射关系</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCodeProductUpsertRequest implements Verifiable {
    /**
     * 适用商品 SPU ID 列表 (必填, 去重后生效)
     */
    private List<Long> productIds;

    /**
     * 校验并规范化字段
     *
     * <p>该方法会:</p>
     * <ul>
     *     <li>校验列表非空且至少包含一个元素</li>
     *     <li>过滤 null 并对 ID 做范围校验</li>
     *     <li>保持顺序去重</li>
     * </ul>
     */
    @Override
    public void validate() {
        requireNotNull(productIds, "productIds 不能为空");
        require(!productIds.isEmpty(), "productIds 不能为空");
        Set<Long> dedup = new LinkedHashSet<>();
        for (Long productId : productIds) {
            if (productId == null)
                continue;
            require(productId >= 1, "productIds 中的元素必须大于等于 1");
            dedup.add(productId);
        }
        require(!dedup.isEmpty(), "productIds 不能为空");
        productIds = List.copyOf(dedup);
    }
}

