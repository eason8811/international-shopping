package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.vo.products.ProductDetail;
import shopping.international.domain.model.vo.products.ProductListQuery;
import shopping.international.domain.model.vo.products.ProductSummary;

import java.util.List;

/**
 * 商品查询服务
 */
public interface IProductQueryService {

    /**
     * 分页查询上架商品
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @NotNull
    PageResult<ProductSummary> list(@NotNull ProductListQuery query);

    /**
     * 根据 slug 获取商品详情 (仅上架)
     *
     * @param slug        商品 slug (可能为多语言)
     * @param locale      语言
     * @param currency    币种
     * @param currentUser 当前用户ID, 可空, 用于 likedAt
     * @return 详情
     */
    @NotNull
    ProductDetail detail(@NotNull String slug, @Nullable String locale, @Nullable String currency, @Nullable Long currentUser);

    /**
     * 简单分页结果
     *
     * @param items 列表
     * @param total 总数
     * @param <T>   类型
     */
    record PageResult<T>(List<T> items, long total) {
    }
}
