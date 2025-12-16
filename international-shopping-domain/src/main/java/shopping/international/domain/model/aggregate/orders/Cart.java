package shopping.international.domain.model.aggregate.orders;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.types.exceptions.IllegalParamException;
import shopping.international.types.utils.Verifiable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 购物车聚合根 (user_id 维度), 对应表 shopping_cart_item
 *
 * <p>聚合职责: 维护 一人一 SKU 一条 的不变式与条目数量/勾选等规则。</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class Cart implements Verifiable {
    /**
     * 用户 ID
     */
    private final Long userId;

    /**
     * 购物车中的条目列表, 用于存储用户购物车内的所有商品条目
     */
    @NotNull
    private final List<CartItem> items = new ArrayList<>();

    /**
     * 构造一个新的购物车实例, 与指定的用户关联
     *
     * @param userId 用户 ID, 必须不为空
     * @throws IllegalParamException 如果 <code>userId</code> 为 <code>null</code>
     */
    private Cart(Long userId) {
        requireNotNull(userId, "用户 ID 不能为空");
        this.userId = userId;
    }

    /**
     * 创建一个新的购物车实例, 并与指定的用户关联
     *
     * @param userId 用户 ID, 必须不为空
     * @return 返回新创建的 {@link Cart} 实例
     * @throws IllegalParamException 如果 <code>userId</code> 为 <code>null</code>
     */
    public static Cart create(Long userId) {
        return new Cart(userId);
    }

    /**
     * 重新构建购物车实例, 基于提供的用户 ID 和条目列表
     *
     * @param userId 用户 ID, 必须不为空
     * @param items  购物车条目列表, 可以为 null
     * @return 返回新创建的 {@link Cart} 实例, 包含给定的用户 ID 和条目列表
     * @throws IllegalParamException 如果 <code>userId</code> 为 <code>null</code>
     */
    public static Cart reconstitute(Long userId, List<CartItem> items) {
        Cart cart = new Cart(userId);
        if (items != null)
            cart.items.addAll(items);
        cart.validate();
        return cart;
    }

    /**
     * 向购物车中添加或更新商品条目
     *
     * @param skuId    商品的 SKU ID, 必须不为空
     * @param quantity 要添加的商品数量, 必须大于 0
     * @param selected 商品是否被选中的状态
     * @return 返回添加或更新后的 {@link CartItem} 实例
     * @throws IllegalParamException 如果 <code>skuId</code> 为 <code>null</code>, 或者 <code>quantity</code> 小于等于 0
     */
    public CartItem addItem(Long skuId, int quantity, boolean selected) {
        requireNotNull(skuId, "SKU ID 不能为空");
        require(quantity > 0, "数量必须大于 0");
        CartItem existing = items.stream()
                .filter(item -> Objects.equals(item.getSkuId(), skuId))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.changeQuantity(quantity);
            existing.setSelected(selected);
            return existing;
        }
        CartItem created = CartItem.create(userId, skuId, quantity, selected);
        items.add(created);
        return created;
    }

    /**
     * 更新指定 ID 的购物车条目信息, 包括数量和选中状态
     *
     * @param itemId   购物车条目的唯一标识符, 必须不为空
     * @param quantity 要更新的商品数量, 可以为 null, 若非空则必须大于 0
     * @param selected 商品是否被选中的状态, 可以为 null
     * @return 返回更新后的 {@link CartItem} 实例
     * @throws IllegalParamException 如果 <code>itemId</code> 为 <code>null</code>, 或者提供的 <code>itemId</code> 在当前购物车中不存在
     */
    public CartItem updateItem(Long itemId, Long skuId, Integer quantity, Boolean selected) {
        requireNotNull(itemId, "购物车条目 ID 不能为空");
        CartItem item = items.stream()
                .filter(it -> Objects.equals(it.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> IllegalParamException.of("购物车条目不存在"));
        if (skuId != null)
            item.changeSkuId(skuId);
        if (quantity != null)
            item.changeQuantity(quantity);
        if (selected != null)
            item.setSelected(selected);
        return item;
    }

    /**
     * 从购物车中移除指定 ID 的条目
     *
     * @param itemId 购物车条目的唯一标识符, 必须不为空
     * @throws IllegalParamException 如果 <code>itemId</code> 为 <code>null</code>, 或者提供的 <code>itemId</code> 在当前购物车中不存在
     */
    public void removeItem(Long itemId) {
        requireNotNull(itemId, "购物车条目 ID 不能为空");
        boolean removed = items.removeIf(it -> Objects.equals(it.getId(), itemId));
        require(removed, "购物车条目不存在");
    }

    /**
     * 返回购物车条目列表, 按照添加时间降序排列
     *
     * <p>此方法对当前购物车中的所有条目按照它们被添加到购物车的时间进行排序,
     * 最新添加的条目将出现在列表的最前面. 如果有多个条目的添加时间为 <code>null</code>,
     * 则这些条目会被视为最后添加, 并且它们之间的顺序保持不变</p>
     *
     * @return 返回一个 {@link List} 包含按添加时间降序排列的购物车条目
     */
    public List<CartItem> listSortedByAddedAtDesc() {
        return items.stream()
                .sorted(Comparator.comparing(CartItem::getAddedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    /**
     * 验证购物车的完整性和合法性
     *
     * <p>此方法执行以下验证:
     * <ul>
     *     <li>确保用户 ID 不为空</li>
     *     <li>遍历所有购物车条目, 确保每个条目不为空并调用其自身的 {@link CartItem#validate()} 方法进行验证</li>
     *     <li>检查每个购物车条目的用户 ID 与当前购物车的用户 ID 是否一致</li>
     *     <li>确保购物车内没有重复的 SKU (即每个 SKU 只能出现一次)</li>
     * </ul>
     * 如果任何一项验证失败, 将抛出 {@link IllegalParamException} 异常</p>
     *
     * @throws IllegalParamException 如果用户 ID 为空, 购物车条目为空, 条目用户 ID 不匹配, 或者存在重复的 SKU
     */
    @Override
    public void validate() {
        requireNotNull(userId, "用户 ID 不能为空");
        for (CartItem item : items) {
            requireNotNull(item, "购物车条目不能为空");
            item.validate();
            require(Objects.equals(item.getUserId(), userId), "购物车条目用户不一致");
        }
        long distinctSkuCount = items.stream().map(CartItem::getSkuId).filter(Objects::nonNull).distinct().count();
        require(distinctSkuCount == items.size(), "购物车同一 SKU 不能重复");
    }
}

