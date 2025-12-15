package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 购物车条目加购/更新请求体 (CartItemUpsertRequest)
 *
 * <p>用于:</p>
 * <ul>
 *     <li>加购: 创建或幂等更新同一用户同一 SKU 的购物车条目</li>
 *     <li>更新: 修改购物车条目的数量与勾选状态</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemUpsertRequest implements Verifiable {
    /**
     * SKU ID
     */
    @Nullable
    private Long skuId;
    /**
     * 数量 (最小为 1)
     */
    @Nullable
    private Integer quantity;
    /**
     * 是否勾选 (创建时默认 true; 更新时为 null 表示不更新)
     */
    @Nullable
    private Boolean selected;

    /**
     * 通用字段校验
     *
     * <p>该方法不会为字段设置默认值, 仅做范围合法性校验</p>
     */
    @Override
    public void validate() {
        if (skuId != null)
            require(skuId >= 1, "skuId 必须大于等于 1");
        if (quantity != null)
            require(quantity >= 1, "quantity 必须大于等于 1");
    }

    /**
     * 加购场景校验
     *
     * <p>创建时必须提供 {@code skuId} 与 {@code quantity}, {@code selected} 默认为 true</p>
     */
    @Override
    public void createValidate() {
        validate();
        requireNotNull(skuId, "skuId 不能为空");
        requireNotNull(quantity, "quantity 不能为空");
        selected = selected == null || selected;
    }

    /**
     * 更新场景校验
     *
     * <p>更新时不允许修改 {@code skuId}; 且至少提供一个需要更新的字段</p>
     */
    @Override
    public void updateValidate() {
        validate();
        require(skuId != null || quantity != null || selected != null, "更新购物车条目时至少需要提供 skuId, quantity 或 selected");
    }
}

