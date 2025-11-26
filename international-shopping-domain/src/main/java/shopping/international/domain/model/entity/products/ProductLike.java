package shopping.international.domain.model.entity.products;

import java.time.LocalDateTime;

/**
 * 商品点赞关系
 *
 * @param userId    用户ID
 * @param productId 商品ID
 * @param likedAt   点赞时间
 */
public record ProductLike(Long userId, Long productId, LocalDateTime likedAt) {
}
