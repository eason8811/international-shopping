package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.products.IProductLikeRepository;
import shopping.international.domain.model.aggregate.products.ProductLike;
import shopping.international.infrastructure.dao.products.ProductLikeMapper;
import shopping.international.infrastructure.dao.products.po.ProductLikePO;

import java.util.Optional;

/**
 * 点赞关系仓储实现
 *
 * <p>基于 MyBatis-Plus, 提供点赞的幂等保存与删除能力</p>
 */
@Repository
@RequiredArgsConstructor
public class ProductLikeRepository implements IProductLikeRepository {

    /**
     * 点赞表 Mapper
     */
    private final ProductLikeMapper productLikeMapper;

    /**
     * 查询用户对商品的点赞关系
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 点赞聚合, 不存在返回空
     */
    @Override
    public @NotNull Optional<ProductLike> find(@NotNull Long userId, @NotNull Long productId) {
        LambdaQueryWrapper<ProductLikePO> wrapper = new LambdaQueryWrapper<ProductLikePO>()
                .eq(ProductLikePO::getUserId, userId)
                .eq(ProductLikePO::getProductId, productId);
        ProductLikePO po = productLikeMapper.selectOne(wrapper);
        if (po == null)
            return Optional.empty();
        return Optional.of(ProductLike.reconstitute(po.getUserId(), po.getProductId(), po.getCreatedAt()));
    }

    /**
     * 保存点赞关系, 若已存在则返回现有记录
     *
     * @param like 点赞聚合
     * @return 持久化后的聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull ProductLike save(@NotNull ProductLike like) {
        ProductLikePO po = ProductLikePO.builder()
                .userId(like.getUserId())
                .productId(like.getProductId())
                .createdAt(like.getLikedAt())
                .build();
        try {
            productLikeMapper.insert(po);
        } catch (DataIntegrityViolationException e) {
            return find(like.getUserId(), like.getProductId())
                    .orElse(ProductLike.reconstitute(like.getUserId(), like.getProductId(), like.getLikedAt()));
        }
        return find(like.getUserId(), like.getProductId())
                .orElse(ProductLike.reconstitute(like.getUserId(), like.getProductId(), like.getLikedAt()));
    }

    /**
     * 取消点赞
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 是否删除了记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(@NotNull Long userId, @NotNull Long productId) {
        LambdaQueryWrapper<ProductLikePO> wrapper = new LambdaQueryWrapper<ProductLikePO>()
                .eq(ProductLikePO::getUserId, userId)
                .eq(ProductLikePO::getProductId, productId);
        return productLikeMapper.delete(wrapper) > 0;
    }
}
