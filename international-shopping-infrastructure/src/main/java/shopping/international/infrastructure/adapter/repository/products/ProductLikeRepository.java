package shopping.international.infrastructure.adapter.repository.products;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import shopping.international.domain.adapter.repository.products.IProductLikeRepository;
import shopping.international.infrastructure.dao.products.ProductLikeMapper;
import shopping.international.infrastructure.dao.products.po.ProductLikePO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品点赞仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ProductLikeRepository implements IProductLikeRepository {

    private final ProductLikeMapper productLikeMapper;

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
}
