package shopping.international.trigger.controller.products;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import shopping.international.api.resp.Result;
import shopping.international.api.resp.products.ProductDetailRespond;
import shopping.international.api.resp.products.ProductRespond;
import shopping.international.domain.model.enums.products.ProductSort;
import shopping.international.domain.model.vo.products.ProductDetail;
import shopping.international.domain.model.vo.products.ProductListQuery;
import shopping.international.domain.model.vo.products.ProductSummary;
import shopping.international.domain.service.products.IProductQueryService;
import shopping.international.types.constant.SecurityConstants;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 商品查询接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/products")
public class ProductController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IProductQueryService productQueryService;

    /**
     * 搜索/筛选商品列表
     */
    @GetMapping
    public ResponseEntity<Result<List<ProductRespond>>> list(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size,
                                                             @RequestParam(value = "locale", required = false) String locale,
                                                             @RequestParam(value = "currency", required = false) String currency,
                                                             @RequestParam(value = "category_slug", required = false) String categorySlug,
                                                             @RequestParam(value = "query", required = false) String keyword,
                                                             @RequestParam(value = "tags", required = false) String tags,
                                                             @RequestParam(value = "price_min", required = false) BigDecimal priceMin,
                                                             @RequestParam(value = "price_max", required = false) BigDecimal priceMax,
                                                             @RequestParam(value = "sort_by", required = false) String sortBy) {
        if (page <= 0)
            page = 1;
        if (size <= 0)
            size = 20;
        if (size > 100)
            size = 100;
        List<String> tagList = parseTags(tags);
        ProductListQuery query = new ProductListQuery(page, size, locale, currency, categorySlug, keyword, tagList,
                priceMin, priceMax, ProductSort.from(sortBy), resolveCurrentUserId());
        IProductQueryService.PageResult<ProductSummary> pageResult = productQueryService.list(query);
        List<ProductRespond> data = pageResult.items().stream().map(ProductRespond::from).toList();
        Result.Meta meta = Result.Meta.builder()
                .page(page)
                .size(size)
                .total(pageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Result<ProductDetailRespond>> detail(@PathVariable("slug") String slug,
                                                               @RequestParam(value = "locale", required = false) String locale,
                                                               @RequestParam(value = "currency", required = false) String currency) {
        ProductDetail detail = productQueryService.detail(slug, locale, currency, resolveCurrentUserId());
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, locale)));
    }

    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank())
            return Collections.emptyList();
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("["))
                return OBJECT_MAPPER.readValue(trimmed, new TypeReference<List<String>>() {
                });
        } catch (Exception ignore) {
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Long resolveCurrentUserId() {
        try {
            Authentication authentication = null;
            if (SecurityContextHolder.getContext() != null)
                authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated())
                return null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long longUserId)
                return longUserId;
            if (principal instanceof String stringUserId)
                return Long.parseLong(stringUserId);
        } catch (Exception ignore) {
        }
        return null;
    }
}
