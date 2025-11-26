package shopping.international.trigger.controller.products;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.LikeStateRespond;
import shopping.international.api.resp.products.ProductRespond;
import shopping.international.domain.model.vo.products.LikeState;
import shopping.international.domain.model.vo.products.ProductSummary;
import shopping.international.domain.service.products.IProductLikeService;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AccountException;

import java.util.List;

/**
 * 商品点赞接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX)
public class ProductLikeController {

    /**
     * 点赞服务
     */
    private final IProductLikeService productLikeService;

    /**
     * 点赞商品 (幂等)
     *
     * @param productId 商品ID
     * @return 点赞状态
     */
    @PutMapping("/products/{product_id}/like")
    public ResponseEntity<Result<LikeStateRespond>> like(@PathVariable("product_id") Long productId) {
        Long userId = requireCurrentUserId();
        LikeState state = productLikeService.like(userId, productId);
        return ResponseEntity.ok(Result.ok(LikeStateRespond.from(state)));
    }

    /**
     * 取消点赞商品 (幂等)
     *
     * @param productId 商品ID
     * @return 点赞状态
     */
    @DeleteMapping("/products/{product_id}/like")
    public ResponseEntity<Result<LikeStateRespond>> unlike(@PathVariable("product_id") Long productId) {
        Long userId = requireCurrentUserId();
        LikeState state = productLikeService.unlike(userId, productId);
        return ResponseEntity.ok(Result.ok(LikeStateRespond.from(state)));
    }

    /**
     * 获取当前用户点赞的商品列表
     *
     * @param page     页码
     * @param size     每页数量
     * @param locale   语言
     * @param currency 价格币种
     * @return 商品列表
     */
    @GetMapping("/users/me/likes/products")
    public ResponseEntity<Result<List<ProductRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             @RequestParam(value = "locale", required = false) String locale,
                                                             @RequestParam(value = "currency", required = false) String currency) {
        if (page <= 0)
            page = 1;
        if (size <= 0)
            size = 10;
        if (size > 100)
            size = 100;
        Long userId = requireCurrentUserId();
        IProductQueryService.PageResult<ProductSummary> pageResult = productLikeService.listUserLikes(userId, page, size, locale, currency);
        List<ProductRespond> data = pageResult.items()
                .stream()
                .map(ProductRespond::from)
                .toList();
        Result.Meta meta = Result.Meta.builder()
                .page(page)
                .size(size)
                .total(pageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 解析当前登录用户ID, 未登录抛出异常
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
