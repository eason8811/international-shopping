package shopping.international.domain.model.vo.products;

import shopping.international.domain.model.enums.products.ProductSort;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品列表查询条件
 *
 * @param page         页码 (从1开始)
 * @param size         每页数量
 * @param locale       语言
 * @param currency     币种
 * @param categorySlug 分类 slug
 * @param keyword      关键词
 * @param tags         标签集合
 * @param priceMin     价格下限
 * @param priceMax     价格上限
 * @param sortBy       排序
 * @param userId       当前用户ID (用于 likedAt)
 */
public record ProductListQuery(int page,
                               int size,
                               String locale,
                               String currency,
                               String categorySlug,
                               String keyword,
                               List<String> tags,
                               BigDecimal priceMin,
                               BigDecimal priceMax,
                               ProductSort sortBy,
                               Long userId) {
}
