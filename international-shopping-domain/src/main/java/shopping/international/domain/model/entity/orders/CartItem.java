package shopping.international.domain.model.entity.orders;

import lombok.*;
import lombok.experimental.Accessors;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 购物车条目实体 (对应表 shopping_cart_item), 归属 Cart 聚合
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CartItem implements Verifiable {
    /**
     * 主键
     */
    private Long id;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 购物车条目关联的 SKU ID
     */
    private Long skuId;
    /**
     * 关联的 SKU 数量
     */
    private int quantity;
    /**
     * 购物车条目是否被选中的状态
     */
    @Setter
    private boolean selected;
    /**
     * 购物车条目添加的时间
     */
    private LocalDateTime addedAt;

    /**
     * 创建一个新的购物车条目
     *
     * @param userId   用户 ID, 必须不为空
     * @param skuId    商品的 SKU ID, 必须不为空
     * @param quantity 要添加的商品数量, 必须大于 0
     * @param selected 商品是否被选中的状态
     * @return 返回创建后的 {@link CartItem} 实例
     * @throws IllegalParamException 如果 <code>userId</code> 或 <code>skuId</code> 为 <code>null</code>, 或者 <code>quantity</code> 小于等于 0
     */
    public static CartItem create(Long userId, Long skuId, int quantity, boolean selected) {
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(skuId, "SKU ID 不能为空");
        require(quantity > 0, "数量必须大于 0");
        return new CartItem(null, userId, skuId, quantity, selected, LocalDateTime.now());
    }

    /**
     * 修改当前购物车条目的 SKU ID
     *
     * <p>该方法首先验证传入的 SKU ID 是否为非空且大于 0, 然后更新购物车条目中的 SKU ID</p>
     *
     * @param skuId 新的 SKU ID, 必须是非空且大于 0 的值
     * @throws IllegalParamException 如果传入的 SKU ID 为空或不大于 0
     */
    public void changeSkuId(Long skuId) {
        requireNotNull(skuId, "SKU ID 不能为空");
        require(skuId > 0, "SKU ID 不合法");
        this.skuId = skuId;
    }

    /**
     * 修改当前购物车条目的商品数量
     *
     * <p>该方法首先验证传入的数量是否大于 0, 然后更新购物车条目中的商品数量</p>
     *
     * @param quantity 新的商品数量, 必须是大于 0 的值
     * @throws IllegalParamException 如果传入的数量不大于 0
     */
    public void changeQuantity(int quantity) {
        require(quantity > 0, "数量必须大于 0");
        this.quantity = quantity;
    }

    /**
     * 验证当前购物车条目实体的完整性与合法性
     *
     * @throws IllegalParamException 如果用户 ID 或 SKU ID 为空, 或者商品数量不大于 0
     */
    @Override
    public void validate() {
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(skuId, "SKU ID 不能为空");
        require(quantity > 0, "数量必须大于 0");
    }
}

