package shopping.international.domain.service.orders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车领域服务接口
 *
 * <p>职责:</p>
 * <ul>
 *     <li>面向“当前用户”提供购物车条目的增删改查</li>
 *     <li>提供面向页面展示的分页视图查询</li>
 * </ul>
 */
public interface ICartService {

    /**
     * 购物车条目展示视图 (用于用户侧列表)
     *
     * @param id            购物车条目 ID
     * @param skuId         SKU ID
     * @param quantity      数量
     * @param selected      是否勾选
     * @param addedAt       加购时间
     * @param productId     商品 ID (可为空)
     * @param title         商品标题 (可为空)
     * @param coverImageUrl 商品封面图 (可为空)
     * @param currency      展示币种 (可为空)
     * @param unitPrice     展示单价 (可为空, 金额字符串)
     */
    record CartItemView(Long id, Long skuId, Integer quantity, Boolean selected, LocalDateTime addedAt,
                        Long productId, String title, String coverImageUrl, String currency, String unitPrice) {
    }

    /**
     * 列出当前用户购物车条目 (分页)
     *
     * @param userId   当前用户 ID
     * @param pageQuery 分页参数
     * @param locale   展示语言 (可为空)
     * @param currency 展示币种 (可为空)
     * @return 分页结果
     */
    @NotNull
    PageResult<CartItemView> list(@NotNull Long userId, @NotNull PageQuery pageQuery,
                                  @Nullable String locale, @Nullable String currency);

    /**
     * 加购/幂等更新 (一人一 SKU 一条)
     *
     * @param userId   当前用户 ID
     * @param skuId    SKU ID
     * @param quantity 数量
     * @param selected 是否勾选
     * @return 最新条目快照
     */
    @NotNull
    CartItem addOrUpdate(@NotNull Long userId, @NotNull Long skuId, int quantity, boolean selected);

    /**
     * 更新购物车条目 (字段为 null 表示不更新)
     *
     * @param userId   当前用户 ID
     * @param itemId   条目 ID
     * @param skuId    SKU ID (可选)
     * @param quantity 数量 (可选)
     * @param selected 勾选状态 (可选)
     * @return 更新后的条目快照
     */
    @NotNull
    CartItem update(@NotNull Long userId, @NotNull Long itemId,
                    @Nullable Long skuId, @Nullable Integer quantity, @Nullable Boolean selected);

    /**
     * 删除购物车条目
     *
     * @param userId 当前用户 ID
     * @param itemId 条目 ID
     */
    void delete(@NotNull Long userId, @NotNull Long itemId);
}
