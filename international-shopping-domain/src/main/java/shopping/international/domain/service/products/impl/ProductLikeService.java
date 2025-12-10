package shopping.international.domain.service.products.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.repository.products.IProductLikeRepository;
import shopping.international.domain.adapter.repository.products.IProductRepository;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.ProductLike;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.service.products.IProductLikeService;
import shopping.international.types.exceptions.IllegalParamException;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 商品点赞领域服务实现
 *
 * <p>负责点赞/取消点赞的幂等编排与商品状态校验</p>
 */
@Service
@RequiredArgsConstructor
public class ProductLikeService implements IProductLikeService {

    /**
     * 点赞关系仓储
     */
    private final IProductLikeRepository productLikeRepository;
    /**
     * 商品聚合仓储
     */
    private final IProductRepository productRepository;

    /**
     * 对商品执行点赞(幂等)
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 点赞状态
     */
    @Override
    public @NotNull LikeState like(@NotNull Long userId, @NotNull Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
                .orElseThrow(() -> new IllegalParamException("商品不存在或未上架"));
        require(product.getStatus() == ProductStatus.ON_SALE, "商品不存在或未上架");
        return productLikeRepository.find(userId, productId)
                .map(existing -> LikeState.builder()
                        .liked(Boolean.TRUE)
                        .likedAt(existing.getLikedAt())
                        .build()
                )
                .orElseGet(() -> {
                    ProductLike saved = productLikeRepository.save(ProductLike.like(userId, productId));
                    return LikeState.builder()
                            .liked(Boolean.TRUE)
                            .likedAt(saved.getLikedAt())
                            .build();
                });
    }

    /**
     * 取消商品点赞(幂等)
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return 点赞状态
     */
    @Override
    public @NotNull LikeState cancel(@NotNull Long userId, @NotNull Long productId) {
        productRepository.findById(productId)
                .filter(product -> product.getStatus() == ProductStatus.ON_SALE)
                .orElseThrow(() -> new IllegalParamException("商品不存在或未上架"));
        productLikeRepository.delete(userId, productId);
        return LikeState.builder()
                .liked(Boolean.FALSE)
                .likedAt(null)
                .build();
    }
}
