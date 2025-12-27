package shopping.international.trigger.controller.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.req.orders.CartItemUpsertRequest;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.orders.CartItemRespond;
import shopping.international.domain.model.entity.orders.CartItem;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.service.orders.ICartService;
import shopping.international.types.constant.SecurityConstants;
import shopping.international.types.exceptions.AccountException;

import java.util.List;

/**
 * 购物车接口 {@code /users/me/cart/items}
 *
 * <p>职责:</p>
 * <ul>
 *     <li>列出购物车条目 (分页)</li>
 *     <li>加购 (一人一 SKU 一条, 幂等)</li>
 *     <li>修改条目 (数量/勾选)</li>
 *     <li>删除条目</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/users/me/cart/items")
public class CartController {

    /**
     * 购物车领域服务
     */
    private final ICartService cartService;

    /**
     * 列出购物车条目 (当前用户)
     *
     * @param page     页码 (默认 1)
     * @param size     每页大小 (默认 20)
     * @param locale   展示语言 (可为空)
     * @param currency 展示币种 (可为空)
     * @return 分页结果
     */
    @GetMapping
    public ResponseEntity<Result<List<CartItemRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @RequestParam(required = false) String locale,
                                                              @RequestParam(required = false) String currency) {
        PageQuery pageQuery = PageQuery.of(page, size, 200);
        Long userId = requireCurrentUserId();
        PageResult<ICartService.CartItemView> pageData = cartService.list(userId, pageQuery, locale, currency);
        List<CartItemRespond> data = pageData.items().stream().map(CartController::toRespond).toList();
        return ResponseEntity.ok(Result.ok(
                data,
                Result.Meta.builder()
                        .page(pageQuery.page())
                        .size(pageQuery.size())
                        .total(pageData.total())
                        .build()
        ));
    }

    /**
     * 加购 (一人一 SKU 一条)
     *
     * @param req 请求体
     * @return 最新条目
     */
    @PostMapping
    public ResponseEntity<Result<CartItemRespond>> add(@RequestBody CartItemUpsertRequest req) {
        req.createValidate();
        Long userId = requireCurrentUserId();
        CartItem item = cartService.addOrUpdate(userId, req.getSkuId(), req.getQuantity(), Boolean.TRUE.equals(req.getSelected()));
        return ResponseEntity.ok(Result.ok(toRespond(item)));
    }

    /**
     * 修改购物车条目 (数量/勾选)
     *
     * @param id  条目 ID
     * @param req 请求体
     * @return 最新条目
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Result<CartItemRespond>> update(@PathVariable("id") Long id, @RequestBody CartItemUpsertRequest req) {
        req.updateValidate();
        Long userId = requireCurrentUserId();
        CartItem updated = cartService.update(userId, id, req.getSkuId(), req.getQuantity(), req.getSelected());
        return ResponseEntity.ok(Result.ok(toRespond(updated)));
    }

    /**
     * 删除购物车条目
     *
     * @param id 条目 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable("id") Long id) {
        Long userId = requireCurrentUserId();
        cartService.delete(userId, id);
        return ResponseEntity.ok(Result.ok("购物车条目已删除"));
    }

    /**
     * CartItemView → CartItemRespond
     *
     * @param view 视图
     * @return 响应
     */
    private static CartItemRespond toRespond(ICartService.CartItemView view) {
        return CartItemRespond.builder()
                .id(view.id())
                .skuId(view.skuId())
                .quantity(view.quantity())
                .selected(view.selected())
                .addedAt(view.addedAt())
                .productId(view.productId())
                .title(view.title())
                .coverImageUrl(view.coverImageUrl())
                .currency(view.currency())
                .unitPrice(view.unitPrice())
                .build();
    }

    /**
     * CartItem → CartItemRespond
     *
     * @param item 条目实体
     * @return 响应
     */
    private static CartItemRespond toRespond(CartItem item) {
        return CartItemRespond.builder()
                .id(item.getId())
                .skuId(item.getSkuId())
                .quantity(item.getQuantity())
                .selected(item.isSelected())
                .addedAt(item.getAddedAt())
                .build();
    }

    /**
     * 从安全上下文中解析当前用户 ID
     *
     * @return 当前用户 ID
     * @throws AccountException 未登录或无法解析
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
