package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.ProductLike;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品点赞关系仓储
 */
public interface IProductLikeRepository {

    /**
     * 简单分页返回
     */
    record PageResult(List<ProductLike> items, long total) {
    }

    /**
     * 点赞 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 点赞时间
     */
    @NotNull
    LocalDateTime like(@NotNull Long userId, @NotNull Long productId);

    /**
     * 取消点赞 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 是否删除了记录
     */
    boolean unlike(@NotNull Long userId, @NotNull Long productId);

    /**
     * 分页查询用户点赞记录
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 点赞记录分页
     */
    @NotNull
    PageResult pageLikes(@NotNull Long userId, int page, int size);

    /**
     * 查询用户对商品的点赞时间
     *
     * @param userId     用户ID
     * @param productIds 商品ID集合
     * @return productId -> likedAt
     */
    @NotNull
    Map<Long, LocalDateTime> mapLikedAt(@NotNull Long userId, @NotNull Set<Long> productIds);
}
