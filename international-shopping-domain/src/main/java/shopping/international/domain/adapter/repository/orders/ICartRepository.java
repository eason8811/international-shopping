package shopping.international.domain.adapter.repository.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.service.orders.ICartService;

import java.util.List;
import java.util.Optional;

/**
 * 购物车聚合仓储接口 (面向订单域)
 *
 * <p>职责:</p>
 * <ul>
 *     <li>对 {@code shopping_cart_item} 提供聚合粒度的读写能力</li>
 *     <li>提供用户侧购物车展示所需的视图查询 (可联表)</li>
 *     <li>为订单创建 (source=CART) 提供 "勾选条目" 读取与删除能力</li>
 * </ul>
 */
public interface ICartRepository {

    /**
     * 按用户分页查询购物车条目视图
     *
     * @param userId   用户 ID
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页数量
     * @param locale   展示语言, 可为空
     * @param currency 展示币种, 可为空
     * @return 购物车条目视图列表
     */
    @NotNull
    List<ICartService.CartItemView> pageViewsByUser(@NotNull Long userId, int offset, int limit,
                                                    @Nullable String locale, @Nullable String currency);

    /**
     * 统计用户购物车条目总数
     *
     * @param userId 用户 ID
     * @return 条目总数
     */
    long countByUser(@NotNull Long userId);

    /**
     * 查询用户 "勾选" 的购物车条目列表 (用于下单)
     *
     * @param userId 用户 ID
     * @return 勾选条目列表
     */
    @NotNull
    List<CartItem> listSelectedItems(@NotNull Long userId);

    /**
     * 按用户 + 条目 ID 查询购物车条目
     *
     * @param userId 用户 ID
     * @param itemId 条目 ID
     * @return 若存在返回 Optional
     */
    @NotNull
    Optional<CartItem> findById(@NotNull Long userId, @NotNull Long itemId);

    /**
     * 按 (userId, skuId) 幂等加购/更新
     *
     * @param userId   用户 ID
     * @param skuId    SKU ID
     * @param quantity 数量
     * @param selected 是否勾选
     * @return 最新条目快照
     */
    @NotNull
    CartItem upsert(@NotNull Long userId, @NotNull Long skuId, int quantity, boolean selected);

    /**
     * 更新购物车条目 (字段为 null 表示不更新)
     *
     * @param userId   用户 ID
     * @param itemId   条目 ID
     * @param skuId    SKU ID (可选)
     * @param quantity 数量 (可选)
     * @param selected 是否勾选 (可选)
     * @return 更新后的条目快照
     */
    @NotNull
    CartItem update(@NotNull Long userId, @NotNull Long itemId,
                    @Nullable Long skuId, @Nullable Integer quantity, @Nullable Boolean selected);

    /**
     * 删除指定购物车条目
     *
     * @param userId 用户 ID
     * @param itemId 条目 ID
     */
    void delete(@NotNull Long userId, @NotNull Long itemId);

    /**
     * 按 ID 列表批量删除购物车条目 (用于下单后清理)
     *
     * @param userId  用户 ID
     * @param itemIds 待删除条目 ID 列表
     */
    void deleteByIds(@NotNull Long userId, @NotNull List<Long> itemIds);
}

