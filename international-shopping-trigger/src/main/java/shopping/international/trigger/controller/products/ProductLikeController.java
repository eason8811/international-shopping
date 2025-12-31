package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.products.UserLikeProductPageRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.LikeStateRespond;
import shopping.international.api.resp.products.ProductImageRespond;
import shopping.international.api.resp.products.ProductSpuRespond;
import shopping.international.domain.model.enums.products.ProductSort;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.products.ProductPublicSnapshot;
import shopping.international.domain.model.vo.products.ProductSearchCriteria;
import shopping.international.domain.service.common.ICurrencyConfigService;
import shopping.international.domain.service.products.IProductLikeService;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.currency.CurrencyConfig;
import shopping.international.types.exceptions.AccountException;

import java.util.Collections;
import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 商品点赞接口
 *
 * <p>覆盖点赞/取消点赞以及查询当前用户点赞的商品列表</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX)
public class ProductLikeController {

    /**
     * 点赞领域服务
     */
    private final IProductLikeService productLikeService;
    /**
     * 商品浏览领域服务
     */
    private final IProductQueryService productQueryService;
    /**
     * 货币配置服务（用于最小货币单位换算）
     */
    private final ICurrencyConfigService currencyConfigService;

    /**
     * 点赞商品(幂等)
     *
     * @param productId 商品 ID
     * @return 点赞状态
     */
    @PutMapping("/products/{product_id}/like")
    public ResponseEntity<Result<LikeStateRespond>> like(@PathVariable("product_id") Long productId) {
        Long userId = requireCurrentUserId();
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(productId, "商品 ID 不能为空");
        IProductLikeService.LikeState state = productLikeService.like(userId, productId);
        LikeStateRespond respond = LikeStateRespond.builder()
                .liked(state.liked())
                .likedAt(state.likedAt())
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 取消点赞(幂等)
     *
     * @param productId 商品 ID
     * @return 点赞状态
     */
    @DeleteMapping("/products/{product_id}/like")
    public ResponseEntity<Result<LikeStateRespond>> cancel(@PathVariable("product_id") Long productId) {
        Long userId = requireCurrentUserId();
        requireNotNull(userId, "用户 ID 不能为空");
        requireNotNull(productId, "商品 ID 不能为空");
        IProductLikeService.LikeState state = productLikeService.cancel(userId, productId);
        LikeStateRespond respond = LikeStateRespond.builder()
                .liked(state.liked())
                .likedAt(state.likedAt())
                .build();
        return ResponseEntity.ok(Result.ok(respond));
    }

    /**
     * 查询我点赞的商品列表
     *
     * @param req 分页请求
     * @return 商品列表
     */
    @GetMapping("/users/me/likes/products")
    public ResponseEntity<Result<List<ProductSpuRespond>>> myLikes(@ModelAttribute UserLikeProductPageRequest req) {
        req.validate();
        Long userId = requireCurrentUserId();
        requireNotNull(userId, "用户 ID 不能为空");
        CurrencyConfig currencyConfig = currencyConfigService.get(req.getCurrency());
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .locale(req.getLocale())
                .currency(req.getCurrency())
                .sort(ProductSort.LATEST)
                .tags(Collections.emptyList())
                .build();
        PageQuery pageQuery = PageQuery.of(req.getPage(), req.getSize(), 200);
        PageResult<ProductPublicSnapshot> pageResult = productQueryService.pageUserLikes(userId, pageQuery, criteria);
        List<ProductSpuRespond> data = pageResult.items().stream()
                .map(snapshot -> toSpuRespond(snapshot, currencyConfig))
                .toList();
        Result.Meta meta = Result.Meta.builder()
                .page(req.getPage())
                .size(req.getSize())
                .total(pageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 将商品快照转换为列表响应
     *
     * @param snapshot 商品快照
     * @return 列表响应
     */
    private ProductSpuRespond toSpuRespond(@NotNull ProductPublicSnapshot snapshot) {
        CurrencyConfig currencyConfig = currencyConfigService.get(snapshot.getPriceRange().getCurrency());
        return toSpuRespond(snapshot, currencyConfig);
    }

    private ProductSpuRespond toSpuRespond(@NotNull ProductPublicSnapshot snapshot, @NotNull CurrencyConfig currencyConfig) {
        List<ProductImageRespond> gallery = snapshot.getGallery().stream()
                .map(img -> ProductImageRespond.builder()
                        .url(img.getUrl())
                        .isMain(img.isMain())
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();
        return ProductSpuRespond.builder()
                .id(snapshot.getId())
                .slug(snapshot.getSlug())
                .title(snapshot.getTitle())
                .subtitle(snapshot.getSubtitle())
                .description(snapshot.getDescription())
                .categoryId(snapshot.getCategoryId())
                .categorySlug(snapshot.getCategorySlug())
                .brand(snapshot.getBrand())
                .coverImageUrl(snapshot.getCoverImageUrl())
                .stockTotal(snapshot.getStockTotal())
                .saleCount(snapshot.getSaleCount())
                .skuType(snapshot.getSkuType())
                .status(snapshot.getStatus())
                .tags(snapshot.getTags())
                .priceRange(ProductSpuRespond.ProductPriceRangeRespond.builder()
                        .currency(snapshot.getPriceRange().getCurrency())
                        .listPriceMin(currencyConfig.toMajorNullable(snapshot.getPriceRange().getListPriceMin()))
                        .listPriceMax(currencyConfig.toMajorNullable(snapshot.getPriceRange().getListPriceMax()))
                        .salePriceMin(currencyConfig.toMajorNullable(snapshot.getPriceRange().getSalePriceMin()))
                        .salePriceMax(currencyConfig.toMajorNullable(snapshot.getPriceRange().getSalePriceMax()))
                        .build())
                .gallery(gallery)
                .likedAt(snapshot.getLikedAt())
                .build();
    }

    /**
     * 获取当前登录用户 ID, 未登录抛出异常
     *
     * @return 当前用户 ID
     */
    private Long requireCurrentUserId() {
        Authentication authentication = null;
        if (SecurityContextHolder.getContext() != null)
            authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccountException("未登录");
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long longUserId)
            return longUserId;
        if (principal instanceof String stringUserId)
            return Long.parseLong(stringUserId);
        throw new AccountException("无法解析当前用户");
    }
}
