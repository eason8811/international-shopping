package shopping.international.domain.service.products;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * 商品点赞领域服务接口
 *
 * <p>负责点赞/取消点赞等行为编排, 保持幂等与业务校验</p>
 */
public interface IProductLikeService {

    /**
     * 点赞状态读模型
     *
     * @param liked   是否已点赞
     * @param likedAt 点赞时间
     */
    @Builder
    record LikeState(@NotNull Boolean liked, LocalDateTime likedAt) {
    }

    /**
     * 对商品执行点赞(幂等)
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 点赞状态
     */
    @NotNull
    LikeState like(@NotNull Long userId, @NotNull Long productId);

    /**
     * 取消商品点赞(幂等)
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 点赞状态
     */
    @NotNull
    LikeState cancel(@NotNull Long userId, @NotNull Long productId);
}
