package shopping.international.infrastructure.adapter.repository.orders;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.orders.ICartRepository;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.service.orders.ICartService;
import shopping.international.infrastructure.dao.orders.ShoppingCartItemMapper;
import shopping.international.infrastructure.dao.orders.po.CartItemViewPO;
import shopping.international.infrastructure.dao.orders.po.ShoppingCartItemPO;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的购物车仓储实现
 *
 * <p>职责:</p>
 * <ul>
 *     <li>对 {@code shopping_cart_item} 的读写</li>
 *     <li>对用户侧购物车展示视图的联表查询</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class CartRepository implements ICartRepository {

    /**
     * 购物车条目 Mapper
     */
    private final ShoppingCartItemMapper cartItemMapper;

    /**
     * 按用户分页查询购物车条目视图
     *
     * @param userId   用户 ID
     * @param offset   偏移量
     * @param limit    单页数量
     * @param locale   展示语言
     * @param currency 展示币种
     * @return 视图列表
     */
    @Override
    public @NotNull List<ICartService.CartItemView> pageViewsByUser(@NotNull Long userId, int offset, int limit,
                                                                    @Nullable String locale, @Nullable String currency) {
        List<CartItemViewPO> pos = cartItemMapper.selectCartItemViews(userId, offset, limit, locale, currency);
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream()
                .map(po -> new ICartService.CartItemView(
                        po.getId(),
                        po.getSkuId(),
                        po.getQuantity(),
                        po.getSelected(),
                        po.getAddedAt(),
                        po.getProductId(),
                        po.getTitle(),
                        po.getCoverImageUrl(),
                        po.getCurrency(),
                        po.getUnitPrice() == null ? null : po.getUnitPrice().toPlainString()
                ))
                .toList();
    }

    /**
     * 统计用户购物车条目数量
     *
     * @param userId 用户 ID
     * @return 总数
     */
    @Override
    public long countByUser(@NotNull Long userId) {
        return cartItemMapper.selectCount(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getUserId, userId));
    }

    /**
     * 查询用户勾选条目列表
     *
     * @param userId 用户 ID
     * @return 勾选条目
     */
    @Override
    public @NotNull List<CartItem> listSelectedItems(@NotNull Long userId) {
        List<ShoppingCartItemPO> pos = cartItemMapper.selectList(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getUserId, userId)
                .eq(ShoppingCartItemPO::getSelected, Boolean.TRUE)
                .orderByDesc(ShoppingCartItemPO::getAddedAt));
        if (pos == null || pos.isEmpty())
            return List.of();
        return pos.stream().map(this::toEntity).toList();
    }

    /**
     * 按用户 + 条目 ID 查询
     *
     * @param userId 用户 ID
     * @param itemId 条目 ID
     * @return Optional
     */
    @Override
    public @NotNull Optional<CartItem> findById(@NotNull Long userId, @NotNull Long itemId) {
        ShoppingCartItemPO po = cartItemMapper.selectOne(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getId, itemId)
                .eq(ShoppingCartItemPO::getUserId, userId)
                .last("limit 1"));
        return po == null ? Optional.empty() : Optional.of(toEntity(po));
    }

    /**
     * 幂等加购/更新
     *
     * @param userId   用户 ID
     * @param skuId    SKU ID
     * @param quantity 数量
     * @param selected 是否勾选
     * @return 最新条目快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull CartItem upsert(@NotNull Long userId, @NotNull Long skuId, int quantity, boolean selected) {
        ShoppingCartItemPO existing = cartItemMapper.selectOne(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getUserId, userId)
                .eq(ShoppingCartItemPO::getSkuId, skuId)
                .last("limit 1"));
        if (existing != null)
            return updateCartItem(userId, existing.getQuantity() + quantity, selected, existing);

        ShoppingCartItemPO po = ShoppingCartItemPO.builder()
                .userId(userId)
                .skuId(skuId)
                .quantity(quantity)
                .selected(selected)
                .build();
        try {
            cartItemMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            // 并发下 unique(user_id, sku_id) 冲突, 退化为“更新并回读”
            ShoppingCartItemPO again = cartItemMapper.selectOne(new LambdaQueryWrapper<ShoppingCartItemPO>()
                    .eq(ShoppingCartItemPO::getUserId, userId)
                    .eq(ShoppingCartItemPO::getSkuId, skuId)
                    .last("limit 1"));
            if (again == null)
                throw e;
            return updateCartItem(userId, again.getQuantity() + quantity, selected, again);
        }
        ShoppingCartItemPO latest = cartItemMapper.selectById(po.getId());
        return toEntity(latest);
    }

    /**
     * 更新购物车条目
     *
     * @param userId   用户 ID
     * @param itemId   条目 ID
     * @param skuId    SKU ID
     * @param quantity 数量
     * @param selected 是否勾选
     * @return 更新后的条目快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull CartItem update(@NotNull Long userId, @NotNull Long itemId,
                                    @Nullable Long skuId, @Nullable Integer quantity, @Nullable Boolean selected) {
        ShoppingCartItemPO existing = cartItemMapper.selectOne(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getId, itemId)
                .eq(ShoppingCartItemPO::getUserId, userId)
                .last("limit 1"));
        if (existing == null)
            throw new IllegalParamException("购物车条目不存在");

        LambdaUpdateWrapper<ShoppingCartItemPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShoppingCartItemPO::getId, itemId)
                .eq(ShoppingCartItemPO::getUserId, userId);
        if (skuId != null)
            wrapper.set(ShoppingCartItemPO::getSkuId, skuId);
        if (quantity != null)
            wrapper.set(ShoppingCartItemPO::getQuantity, quantity);
        if (selected != null)
            wrapper.set(ShoppingCartItemPO::getSelected, selected);
        try {
            cartItemMapper.update(null, wrapper);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("购物车条目唯一约束冲突", e);
        }

        ShoppingCartItemPO latest = cartItemMapper.selectById(itemId);
        return toEntity(latest);
    }

    /**
     * 删除购物车条目
     *
     * @param userId 用户 ID
     * @param itemId 条目 ID
     */
    @Override
    public void delete(@NotNull Long userId, @NotNull Long itemId) {
        int deleted = cartItemMapper.delete(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getId, itemId)
                .eq(ShoppingCartItemPO::getUserId, userId));
        if (deleted <= 0)
            throw new IllegalParamException("购物车条目不存在");
    }

    /**
     * 批量删除购物车条目
     *
     * @param userId  用户 ID
     * @param itemIds 条目 ID 列表
     */
    @Override
    public void deleteByIds(@NotNull Long userId, @NotNull List<Long> itemIds) {
        if (itemIds.isEmpty())
            return;
        cartItemMapper.delete(new LambdaQueryWrapper<ShoppingCartItemPO>()
                .eq(ShoppingCartItemPO::getUserId, userId)
                .in(ShoppingCartItemPO::getId, itemIds));
    }

    /**
     * 更新指定用户的购物车条目信息
     *
     * @param userId   用户 ID, 必须不为空
     * @param quantity 购物车条目的数量, 必须大于 0
     * @param selected 购物车条目是否被选中的状态
     * @param existing 现存的购物车条目持久化对象
     * @return 返回更新后的 {@link CartItem} 实体
     */
    @NotNull
    private CartItem updateCartItem(@NotNull Long userId, int quantity, boolean selected, ShoppingCartItemPO existing) {
        LambdaUpdateWrapper<ShoppingCartItemPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ShoppingCartItemPO::getId, existing.getId())
                .eq(ShoppingCartItemPO::getUserId, userId)
                .set(ShoppingCartItemPO::getQuantity, quantity)
                .set(ShoppingCartItemPO::getSelected, selected);
        cartItemMapper.update(null, wrapper);
        ShoppingCartItemPO latest = cartItemMapper.selectById(existing.getId());
        return toEntity(latest);
    }


    /**
     * PO → Entity
     *
     * @param po 持久化对象
     * @return 领域实体
     */
    private CartItem toEntity(ShoppingCartItemPO po) {
        return new CartItem(
                po.getId(),
                po.getUserId(),
                po.getSkuId(),
                po.getQuantity() == null ? 0 : po.getQuantity(),
                Boolean.TRUE.equals(po.getSelected()),
                po.getAddedAt()
        );
    }
}

