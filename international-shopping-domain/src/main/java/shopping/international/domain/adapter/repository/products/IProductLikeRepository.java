package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 商品点赞关系仓储
 */
public interface IProductLikeRepository {

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
