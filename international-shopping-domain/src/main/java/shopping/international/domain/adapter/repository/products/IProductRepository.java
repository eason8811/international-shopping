package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Product;

import java.util.Optional;

/**
 * 商品聚合仓储接口
 *
 * <p>封装对商品基础信息、图库、规格及多语言的组合读写, 并提供按 slug 定位商品的能力。</p>
 */
public interface IProductRepository {

    /**
     * 按 ID 查询商品聚合
     *
     * @param productId 商品 ID
     * @return 商品聚合, 不存在返回空
     */
    @NotNull
    Optional<Product> findById(@NotNull Long productId);

    /**
     * 按 slug 查询上架商品
     *
     * @param slug   商品 slug 或本地化 slug
     * @param locale 请求语言, 可用于匹配 i18n slug
     * @return 上架商品聚合, 不存在或未上架返回空
     */
    @NotNull
    Optional<Product> findOnSaleBySlug(@NotNull String slug, @NotNull String locale);

    /**
     * 查询分类 slug
     *
     * @param categoryId 分类 ID
     * @return 分类 slug, 不存在返回 null
     */
    @Nullable
    String findCategorySlug(@NotNull Long categoryId);

    /**
     * 更新商品的默认 SKU ID
     *
     * @param productId   商品 ID
     * @param defaultSkuId 默认 SKU ID, 可为空
     */
    void updateDefaultSkuId(@NotNull Long productId, @Nullable Long defaultSkuId);

    /**
     * 覆盖更新商品聚合库存
     *
     * @param productId 商品 ID
     * @param stock     新聚合库存
     */
    void updateStockTotal(@NotNull Long productId, int stock);
}
