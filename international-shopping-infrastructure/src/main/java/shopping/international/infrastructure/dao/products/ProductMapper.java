package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.infrastructure.dao.products.po.ProductPO;
import shopping.international.infrastructure.dao.products.po.PublicProductSnapshotPO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper: product
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {

    /**
     * 聚合读取商品（含图库、规格/规格值及多语言）
     *
     * @param productId 商品ID
     * @return 聚合 PO
     */
    ProductPO selectAggregateById(@Param("productId") Long productId);

    /**
     * 按 slug（含本地化 slug）查询上架商品聚合
     *
     * @param slug   slug 或本地化 slug
     * @param locale 请求语言
     * @return 商品聚合 PO
     */
    ProductPO selectOnSaleAggregateBySlug(@Param("slug") String slug, @Param("locale") String locale);

    /**
     * 为管理员查询商品聚合页面信息, 支持分页及多种条件过滤
     *
     * @param status         商品状态, 可为空
     * @param skuType        SKU类型, 可为空
     * @param categoryId     分类ID, 可为空
     * @param keyword        搜索关键词, 可为空
     * @param tag            标签, 可为空
     * @param includeDeleted 是否包含已删除的商品
     * @param offset         偏移量, 用于分页
     * @param limit          每页数量限制
     * @return 符合条件的商品列表
     */
    List<ProductPO> selectAdminAggregatePage(@Nullable ProductStatus status, @Nullable SkuType skuType,
                                             @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag,
                                             boolean includeDeleted, int offset, int limit);

    /**
     * 统计符合条件的商品聚合页面信息的数量, 用于管理员查询商品列表时的分页需求
     *
     * @param status         商品状态, 可为空
     * @param skuType        SKU类型, 可为空
     * @param categoryId     分类ID, 可为空
     * @param keyword        搜索关键词, 可为空
     * @param tag            标签, 可为空
     * @param includeDeleted 是否包含已删除的商品
     * @return 符合条件的商品总数
     */
    Long countAdminAggregatePage(@Nullable ProductStatus status, @Nullable SkuType skuType,
                                 @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag,
                                 boolean includeDeleted);

    /**
     * 用户侧上架商品分页查询
     *
     * @param locale       语言代码
     * @param currency     币种
     * @param categorySlug 分类 slug, 可空
     * @param keyword      关键词, 可空
     * @param tags         标签列表, 可空
     * @param priceMin     价格下限, 可空
     * @param priceMax     价格上限, 可空
     * @param sort         排序方式
     * @param offset       偏移量
     * @param limit        单页数量
     * @return 商品快照列表
     */
    List<PublicProductSnapshotPO> selectPublicList(@Param("locale") String locale,
                                                   @Param("currency") String currency,
                                                   @Param("categorySlug") String categorySlug,
                                                   @Param("keyword") String keyword,
                                                   @Param("tags") List<String> tags,
                                                   @Param("priceMin") BigDecimal priceMin,
                                                   @Param("priceMax") BigDecimal priceMax,
                                                   @Param("sort") String sort,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    /**
     * 统计上架商品数量
     *
     * @param locale       语言
     * @param currency     币种
     * @param categorySlug 分类 slug
     * @param keyword      关键词
     * @param tags         标签列表
     * @param priceMin     价格下限
     * @param priceMax     价格上限
     * @return 总数
     */
    Long countPublicList(@Param("locale") String locale,
                         @Param("currency") String currency,
                         @Param("categorySlug") String categorySlug,
                         @Param("keyword") String keyword,
                         @Param("tags") List<String> tags,
                         @Param("priceMin") BigDecimal priceMin,
                         @Param("priceMax") BigDecimal priceMax);

    /**
     * 用户点赞商品分页查询
     *
     * @param userId   用户 ID
     * @param locale   语言
     * @param currency 币种
     * @param offset   偏移量
     * @param limit    单页数量
     * @return 点赞商品快照列表
     */
    List<PublicProductSnapshotPO> selectUserLikedList(@Param("userId") Long userId,
                                                      @Param("locale") String locale,
                                                      @Param("currency") String currency,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);

    /**
     * 统计用户点赞商品数量
     *
     * @param userId   用户 ID
     * @param locale   语言
     * @param currency 币种
     * @return 点赞商品总数
     */
    Long countUserLikedList(@Param("userId") Long userId,
                            @Param("locale") String locale,
                            @Param("currency") String currency);
}
