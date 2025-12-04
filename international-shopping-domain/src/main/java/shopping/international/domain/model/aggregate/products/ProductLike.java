package shopping.international.domain.model.aggregate.products;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 商品点赞聚合根, 对应表 product_like
 *
 * <p>职责: 维护用户对 SPU 的点赞关系 (幂等创建/撤销)</p>
 */
@Getter
@ToString
@Accessors(chain = true)
public class ProductLike implements Verifiable {
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 商品 SPU ID
     */
    private Long productId;
    /**
     * 点赞时间
     */
    private LocalDateTime likedAt;

    /**
     * 私有构造函数
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param likedAt   点赞时间
     */
    private ProductLike(Long userId, Long productId, LocalDateTime likedAt) {
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(productId, "商品 ID 不能为空");
        this.userId = userId;
        this.productId = productId;
        this.likedAt = likedAt == null ? LocalDateTime.now() : likedAt;
    }

    /**
     * 创建点赞关系
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 新的点赞聚合根
     */
    public static ProductLike like(Long userId, Long productId) {
        return new ProductLike(userId, productId, LocalDateTime.now());
    }

    /**
     * 重建点赞关系
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param likedAt   点赞时间
     * @return 重建后的点赞聚合根
     */
    public static ProductLike reconstitute(Long userId, Long productId, LocalDateTime likedAt) {
        return new ProductLike(userId, productId, likedAt);
    }

    /**
     * 更新点赞时间 (幂等覆盖)
     *
     * @param likedAt 新的点赞时间
     */
    public void refreshLikedAt(LocalDateTime likedAt) {
        requireNotNull(likedAt, "点赞时间不能为空");
        this.likedAt = likedAt;
    }

    /**
     * 校验点赞关系
     */
    @Override
    public void validate() {
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(productId, "商品 ID 不能为空");
        requireNotNull(likedAt, "点赞时间不能为空");
    }
}
