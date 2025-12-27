package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.aggregate.products.ProductLike;

import java.util.Optional;

/**
 * 商品点赞关系仓储接口
 *
 * <p>封装对 product_like 的增删查, 保证点赞/取消点赞的幂等性</p>
 */
public interface IProductLikeRepository {

    /**
     * 查询用户对商品的点赞关系
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 点赞聚合, 不存在返回空
     */
    @NotNull
    Optional<ProductLike> find(@NotNull Long userId, @NotNull Long productId);

    /**
     * 保存点赞关系, 若已存在则返回现有记录
     *
     * @param like 点赞聚合
     * @return 持久化后的聚合
     */
    @NotNull
    ProductLike save(@NotNull ProductLike like);

    /**
     * 取消点赞
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 是否删除了记录
     */
    boolean delete(@NotNull Long userId, @NotNull Long productId);
}
