package shopping.international.domain.adapter.repository.products;

import org.jetbrains.annotations.NotNull;
import shopping.international.domain.model.entity.products.Product;
import shopping.international.domain.model.vo.products.ProductI18n;
import shopping.international.domain.model.vo.products.ProductImage;

import java.util.*;

/**
 * 商品管理仓储接口
 *
 * <p>负责 product 及其 i18n、图库的持久化读写, 供管理端使用</p>
 */
public interface IProductAdminRepository {
    /**
     * 分页查询商品
     *
     * @param page           页码
     * @param size           每页大小
     * @param status         状态过滤, 可空
     * @param skuType        SKU 类型过滤, 可空
     * @param categoryId     分类过滤, 可空
     * @param keyword        关键词, 可空
     * @param tag            标签, 可空
     * @param includeDeleted 是否包含已删除
     * @return 分页结果
     */
    @NotNull
    PageResult<Product> page(int page, int size, String status, String skuType, Long categoryId, String keyword, String tag, boolean includeDeleted);

    /**
     * 按 ID 查询商品
     *
     * @param productId 商品 ID
     * @return 商品
     */
    @NotNull
    Optional<Product> findById(@NotNull Long productId);

    /**
     * 批量查询商品
     *
     * @param productIds 商品 ID 集合
     * @return 商品映射
     */
    @NotNull
    Map<Long, Product> mapByIds(@NotNull Set<Long> productIds);

    /**
     * 批量查询商品多语言
     *
     * @param productIds 商品 ID 集合
     * @return productId -> 多语言列表
     */
    @NotNull
    Map<Long, List<ProductI18n>> mapI18n(@NotNull Collection<Long> productIds);

    /**
     * 获取商品图库
     *
     * @param productId 商品 ID
     * @return 图库列表
     */
    @NotNull
    List<ProductImage> listGallery(@NotNull Long productId);

    /**
     * 批量获取商品图库
     *
     * @param productIds 商品 ID
     * @return productId -> 图库
     */
    @NotNull
    Map<Long, List<ProductImage>> mapGallery(@NotNull Collection<Long> productIds);

    /**
     * 新增商品
     *
     * @param product 商品实体
     * @return 生成的主键 ID
     */
    @NotNull
    Long insert(@NotNull Product product);

    /**
     * 更新商品
     *
     * @param product 商品实体
     */
    void update(@NotNull Product product);

    /**
     * 批量 upsert 商品多语言
     *
     * @param productId 商品 ID
     * @param payloads  多语言列表
     */
    void upsertI18n(@NotNull Long productId, @NotNull List<ProductI18n> payloads);

    /**
     * 覆盖商品图库
     *
     * @param productId 商品 ID
     * @param gallery   图库列表
     */
    void replaceGallery(@NotNull Long productId, @NotNull List<ProductImage> gallery);

    /**
     * 判断基础 slug 是否重复
     *
     * @param slug      slug
     * @param excludeId 排除 ID
     * @return 是否存在
     */
    boolean existsSlug(@NotNull String slug, Long excludeId);

    /**
     * 判断 locale + slug 是否冲突
     *
     * @param locale           语言
     * @param slug             slug
     * @param excludeProductId 排除商品 ID
     * @return 是否存在
     */
    boolean existsLocalizedSlug(@NotNull String locale, @NotNull String slug, Long excludeProductId);

    /**
     * 简单分页结果
     *
     * @param items 数据列表
     * @param total 总数
     * @param <T>   数据类型
     */
    record PageResult<T>(List<T> items, long total) {
    }
}
