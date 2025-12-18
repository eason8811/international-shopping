package shopping.international.domain.service.orders.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.orders.ICartRepository;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.service.orders.ICartService;

import java.util.List;

/**
 * 购物车领域服务默认实现
 *
 * <p>职责:</p>
 * <ul>
 *     <li>面向当前用户提供购物车增删改查</li>
 *     <li>将持久化写入交给 {@link ICartRepository} (事务控制下沉到基础设施层)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CartService implements ICartService {

    /**
     * 购物车仓储
     */
    private final ICartRepository cartRepository;

    /**
     * 列出当前用户购物车条目 (分页)
     *
     * @param userId    当前用户 ID
     * @param pageQuery 分页请求
     * @param locale    展示语言 (可为空)
     * @param currency  展示币种 (可为空)
     * @return 分页结果
     */
    @Override
    public @NotNull PageResult<CartItemView> list(@NotNull Long userId, @NotNull PageQuery pageQuery,
                                                  @Nullable String locale, @Nullable String currency) {
        pageQuery.validate();
        int offset = pageQuery.offset();
        int limit = pageQuery.limit();
        List<CartItemView> items = cartRepository.pageViewsByUser(userId, offset, limit, locale, currency);
        long total = cartRepository.countByUser(userId);
        return new PageResult<>(items, total);
    }

    /**
     * 加购/幂等更新
     *
     * @param userId   当前用户 ID
     * @param skuId    SKU ID
     * @param quantity 数量
     * @param selected 是否勾选
     * @return 最新条目快照
     */
    @Override
    public @NotNull CartItem addOrUpdate(@NotNull Long userId, @NotNull Long skuId, int quantity, boolean selected) {
        return cartRepository.upsert(userId, skuId, quantity, selected);
    }

    /**
     * 更新购物车条目
     *
     * @param userId   当前用户 ID
     * @param itemId   条目 ID
     * @param skuId    SKU ID (可选)
     * @param quantity 数量 (可选)
     * @param selected 勾选状态 (可选)
     * @return 更新后的条目快照
     */
    @Override
    public @NotNull CartItem update(@NotNull Long userId, @NotNull Long itemId,
                                    @Nullable Long skuId, @Nullable Integer quantity, @Nullable Boolean selected) {
        return cartRepository.update(userId, itemId, skuId, quantity, selected);
    }

    /**
     * 删除购物车条目
     *
     * @param userId 当前用户 ID
     * @param itemId 条目 ID
     */
    @Override
    public void delete(@NotNull Long userId, @NotNull Long itemId) {
        cartRepository.delete(userId, itemId);
    }
}
