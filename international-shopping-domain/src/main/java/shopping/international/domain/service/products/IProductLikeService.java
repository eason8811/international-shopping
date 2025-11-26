package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.products.LikeState;
import shopping.international.domain.model.vo.products.ProductSummary;

/**
 * 商品点赞领域服务
 */
public interface IProductLikeService {

    /**
     * 点赞商品 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 点赞状态
     */
    @NotNull
    LikeState like(@NotNull Long userId, @NotNull Long productId);

    /**
     * 取消点赞 (幂等)
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @return 点赞状态
     */
    @NotNull
    LikeState unlike(@NotNull Long userId, @NotNull Long productId);

    /**
     * 分页查询用户点赞的商品
     *
     * @param userId   用户ID
     * @param page     页码
     * @param size     每页数量
     * @param locale   语言
     * @param currency 价格币种
     * @return 商品列表
     */
    @NotNull
    IProductQueryService.PageResult<ProductSummary> listUserLikes(@NotNull Long userId, int page, int size,
                                                                  @Nullable String locale, @Nullable String currency);
}
