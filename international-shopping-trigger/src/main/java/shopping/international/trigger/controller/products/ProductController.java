package shopping.international.trigger.controller.products;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SecurityConstants.API_PREFIX + "/products")
public class ProductController {

    /**
     * JSON序列化/反序列化工具
     */
    private final ObjectMapper objectMapper;
    /**
     * 商品查询服务
     */
    private final IProductQueryService productQueryService;

    /**
     * 搜索/筛选商品列表 根据给定的参数条件返回分页的商品列表数据
     *
     * @param page         当前页码, 默认值 1
     * @param size         每页显示的数量, 默认值 20, 最大不超过 100
     * @param locale       地区标识, 可选参数
     * @param currency     货币类型, 可选参数
     * @param categorySlug 商品分类 slug, 可选参数
     * @param keyword      搜索关键词, 可选参数
     * @param tags         商品标签, 支持 JSON 数组格式或逗号分隔字符串, 可选参数
     * @param priceMin     最低价格, 可选参数
     * @param priceMax     最高价格, 可选参数
     * @param sortBy       排序方式, 可选参数
     * @return 包含分页信息和商品列表的结果集 {@link ResponseEntity} 包裹着 {@link Result}, 其中包含 {@code List<ProductRespond>} 和元数据
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
        // page 和 size 不合法时, 取默认 1 和 20, 最大不超过 100
        if (page <= 0)
            page = 1;
        if (size <= 0)
            size = 20;
        if (size > 100)
            size = 100;
        List<String> tagList = parseTags(tags);
        // 构建查询条件
        ProductListQuery query = new ProductListQuery(page, size, locale, currency, categorySlug, keyword, tagList,
                priceMin, priceMax, ProductSort.from(sortBy), resolveCurrentUserId());
        IProductQueryService.PageResult<ProductSummary> productSummaryPageResult = productQueryService.list(query);
        List<ProductRespond> data = productSummaryPageResult.items()
                .stream()
                .map(ProductRespond::from)
                .toList();
        Result.Meta meta = Result.Meta.builder()
                .page(page)
                .size(size)
                .total(productSummaryPageResult.total())
                .build();
        return ResponseEntity.ok(Result.ok(data, meta));
    }

    /**
     * 获取指定商品的详细信息
     *
     * @param slug  商品标识符, 用于唯一确定一个商品
     * @param locale 地区标识, 可选参数, 用于指定返回数据的语言版本
     * @param currency 货币类型, 可选参数, 用于指定价格展示的货币单位
     * @return 包含商品详情的结果集 {@link ResponseEntity} 包裹着 {@link Result}, 其中包含 {@code ProductDetailRespond} 对象
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Result<ProductDetailRespond>> detail(@PathVariable("slug") String slug,
                                                               @RequestParam(value = "locale", required = false) String locale,
                                                               @RequestParam(value = "currency", required = false) String currency) {
        ProductDetail detail = productQueryService.detail(slug, locale, currency, resolveCurrentUserId());
        return ResponseEntity.ok(Result.ok(ProductDetailRespond.from(detail, locale)));
    }

    // =========================== 私有方法 ===========================

    /**
     * 解析传入的原始标签字符串, 并将其转换为字符串列表
     *
     * <p>此方法支持两种格式的输入:
     * <ul>
     *     <li>JSON 数组格式: 例如 "[tag1, tag2, tag3]", 如果输入符合 JSON 数组格式, 则尝试使用 {@link ObjectMapper} 进行解析</li>
     *     <li>逗号分隔的字符串: 例如 "tag1, tag2, tag3", 如果不是 JSON 格式或 JSON 解析失败, 则将字符串按逗号分割, 并去除每个标签前后的空白字符</li>
     * </ul>
     *
     * @param raw 待解析的原始标签字符串. 可以为 null 或空格组成的字符串.
     * @return 返回一个包含解析后标签的列表. 如果输入为 null 或仅由空白字符组成, 返回空列表.
     */
    private List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank())
            return Collections.emptyList();
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("["))
                return objectMapper.readValue(trimmed, new TypeReference<>() {
                });
        } catch (Exception ex) {
            log.warn("标签 JSON 解析失败, 改用逗号分隔, 原始输入: {}", trimmed, ex);
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 从当前安全上下文中解析并返回已认证用户的 ID
     *
     * <p>该方法尝试从 Spring Security 的 {@link SecurityContextHolder} 中获取当前的认证信息, 并从中提取用户 ID.
     * 用户 ID 可以是 {@code Long} 类型或 {@code String} 类型, 如果为字符串则尝试转换成 {@code Long}.
     * 若无法获取到有效的认证信息或在提取过程中发生异常, 则返回 null.
     *
     * @return 当前已认证用户的 ID, 如果没有可用的认证信息或者提取失败, 返回 null
     */
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
        } catch (Exception ex) {
            log.warn("从安全上下文解析用户ID失败, 视为未登录访问", ex);
        }
        return null;
    }
}
