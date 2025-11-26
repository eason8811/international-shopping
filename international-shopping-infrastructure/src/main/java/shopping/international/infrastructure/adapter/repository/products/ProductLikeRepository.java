package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.products.IProductLikeRepository;
import shopping.international.domain.model.entity.products.ProductLike;
import shopping.international.infrastructure.dao.products.ProductLikeMapper;
import shopping.international.infrastructure.dao.products.po.ProductLikePO;
import shopping.international.types.exceptions.AppException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品点赞仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductLikeRepository implements IProductLikeRepository {

    /**
     * 产品 Like Mapper
     */
    private final ProductLikeMapper productLikeMapper;

    /**
     * 点赞 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 点赞时间
     */
    @Override
    public @NotNull LocalDateTime like(@NotNull Long userId, @NotNull Long productId) {
        ProductLikePO existing = find(userId, productId);
        if (existing != null && existing.getCreatedAt() != null)
            return existing.getCreatedAt();
        try {
            productLikeMapper.insert(
                    ProductLikePO.builder()
                            .userId(userId)
                            .productId(productId)
                            .build()
            );
        } catch (DuplicateKeyException ex) {
            log.warn("忽略重复点赞, userId={}, productId={}", userId, productId, ex);
        }
        ProductLikePO record = find(userId, productId);
        if (record == null || record.getCreatedAt() == null)
            throw new AppException("点赞失败, 请稍后重试");
        return record.getCreatedAt();
    }

    /**
     * 取消点赞 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 是否删除了记录
     */
    @Override
    public boolean unlike(@NotNull Long userId, @NotNull Long productId) {
        int deleted = productLikeMapper.delete(new LambdaQueryWrapper<ProductLikePO>()
                .eq(ProductLikePO::getUserId, userId)
                .eq(ProductLikePO::getProductId, productId));
        return deleted > 0;
    }

    /**
     * 分页查询点赞关系
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 点赞分页
     */
    @Override
    public @NotNull PageResult pageLikes(@NotNull Long userId, int page, int size) {
        long total = productLikeMapper.selectCount(new LambdaQueryWrapper<ProductLikePO>()
                .eq(ProductLikePO::getUserId, userId));
        if (total == 0)
            return new PageResult(Collections.emptyList(), 0);
        int offset = Math.max(page - 1, 0) * size;
        List<ProductLikePO> records = productLikeMapper.selectList(new LambdaQueryWrapper<ProductLikePO>()
                .eq(ProductLikePO::getUserId, userId)
                .orderByDesc(ProductLikePO::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + offset));
        List<ProductLike> items = records.stream()
                .map(this::toEntity)
                .toList();
        return new PageResult(items, total);
    }

    /**
     * 查询用户对商品的点赞时间
     *
     * @param userId     用户ID
     * @param productIds 商品ID集合
     * @return productId -> likedAt
     */
    @Override
    public @NotNull Map<Long, LocalDateTime> mapLikedAt(@NotNull Long userId, @NotNull Set<Long> productIds) {
        if (productIds.isEmpty())
            return Collections.emptyMap();
        return productLikeMapper.selectList(new LambdaQueryWrapper<ProductLikePO>()
                        .eq(ProductLikePO::getUserId, userId)
                        .in(ProductLikePO::getProductId, productIds))
                .stream()
                .collect(Collectors.toMap(ProductLikePO::getProductId, ProductLikePO::getCreatedAt,
                        (existing, ignore) -> existing, LinkedHashMap::new));
    }

    /**
     * 根据用户ID和商品ID查找点赞记录
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 如果存在对应的点赞记录, 则返回 <code>ProductLikePO</code> 对象; 否则返回 <code>null</code>
     */
    private ProductLikePO find(Long userId, Long productId) {
        return productLikeMapper.selectOne(new LambdaQueryWrapper<ProductLikePO>()
                .eq(ProductLikePO::getUserId, userId)
                .eq(ProductLikePO::getProductId, productId)
                .last("LIMIT 1"));
    }

    private ProductLike toEntity(ProductLikePO po) {
        return new ProductLike(po.getUserId(), po.getProductId(), po.getCreatedAt());
    }
}
