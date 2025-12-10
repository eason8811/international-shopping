package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.products.Product;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;
import shopping.international.domain.model.vo.products.ProductPublicSnapshot;
import shopping.international.domain.model.vo.products.ProductSearchCriteria;

import java.util.List;
import java.util.Optional;

/**
 * 商品聚合仓储接口
 *
 * <p>封装对商品基础信息, 图库, 规格及多语言的组合读写, 并提供按 slug 定位商品的能力</p>
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
     * 分页查询上架商品列表
     *
     * @param criteria 检索条件
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页条数
     * @return 商品快照列表
     */
    @NotNull
    List<ProductPublicSnapshot> pageOnSale(@NotNull ProductSearchCriteria criteria, int offset, int limit);

    /**
     * 统计上架商品数量
     *
     * @param criteria 检索条件
     * @return 满足条件的总数
     */
    long countOnSale(@NotNull ProductSearchCriteria criteria);

    /**
     * 分页查询用户点赞的商品
     *
     * @param userId   用户 ID
     * @param criteria 检索条件(主要用于 locale/currency)
     * @param offset   偏移量, 从 0 开始
     * @param limit    单页条数
     * @return 点赞的商品快照列表
     */
    @NotNull
    List<ProductPublicSnapshot> pageUserLikes(@NotNull Long userId, @NotNull ProductSearchCriteria criteria, int offset, int limit);

    /**
     * 统计用户点赞的商品数量
     *
     * @param userId   用户 ID
     * @param criteria 检索条件
     * @return 点赞商品总数
     */
    long countUserLikes(@NotNull Long userId, @NotNull ProductSearchCriteria criteria);

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

    /**
     * 新增商品聚合 (含基础信息与可选图库)
     *
     * @param product 待保存的商品聚合, ID 为空
     * @return 保存后的商品聚合, 携带持久化 ID
     */
    @NotNull
    Product save(@NotNull Product product);

    /**
     * 增量更新商品基础信息
     *
     * @param product       已存在的商品聚合快照
     * @param replaceGallery 是否需要替换图库
     * @return 更新后的商品聚合
     */
    @NotNull
    Product updateBasic(@NotNull Product product, boolean replaceGallery);

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   新图库列表
     */
    void replaceGallery(@NotNull Long productId, @NotNull java.util.List<ProductImage> gallery);

    /**
     * 新增一条商品多语言记录
     *
     * @param productId 商品 ID
     * @param i18n      多语言值对象
     */
    void saveI18n(@NotNull Long productId, @NotNull ProductI18n i18n);

    /**
     * 更新已存在的商品多语言记录
     *
     * @param productId 商品 ID
     * @param i18n      多语言值对象
     */
    void updateI18n(@NotNull Long productId, @NotNull ProductI18n i18n);

    /**
     * 分页查询商品基础信息
     *
     * @param status         商品状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词过滤, 支持标题/slug/品牌, 可空
     * @param tag            标签过滤, 可空
     * @param includeDeleted 是否包含已删除商品
     * @param offset         偏移量, 从 0 开始
     * @param limit          单页大小
     * @return 商品列表
     */
    @NotNull
    java.util.List<Product> list(@Nullable ProductStatus status,
                                  @Nullable SkuType skuType,
                                  @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag,
                                  boolean includeDeleted, int offset, int limit);

    /**
     * 统计分页查询的商品总数
     *
     * @param status         商品状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词过滤, 可空
     * @param tag            标签过滤, 可空
     * @param includeDeleted 是否包含已删除商品
     * @return 总数量
     */
    long count(@Nullable ProductStatus status,
               @Nullable SkuType skuType,
               @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag, boolean includeDeleted);
}
