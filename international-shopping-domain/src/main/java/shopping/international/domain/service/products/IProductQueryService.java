package shopping.international.domain.service.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.aggregate.products.Sku;

import java.util.List;

/**
 * 商品查询领域服务接口
 *
 * <p>面向用户侧商品详情的读取场景, 负责按 slug 聚合商品、规格与 SKU 视图</p>
 */
public interface IProductQueryService {

    /**
     * 商品详情读模型
     *
     * @param product      商品聚合
     * @param categorySlug 分类 slug
     * @param skus         SKU 列表
     */
    record ProductDetail(@NotNull Product product, @Nullable String categorySlug, @NotNull List<Sku> skus) {
    }

    /**
     * 按 slug 查询用户可见的商品详情
     *
     * @param slug     商品 slug 或本地化 slug
     * @param locale   目标语言
     * @param currency 目标币种, 用于价格过滤
     * @return 商品详情读模型
     */
    @NotNull
    ProductDetail getPublicDetail(@NotNull String slug, @NotNull String locale, @NotNull String currency);
}
