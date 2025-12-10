package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.products.ProductStatus;
import shopping.international.domain.model.enums.products.SkuType;
import shopping.international.infrastructure.dao.products.po.ProductPO;

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

    Long countAdminAggregatePage(@Nullable ProductStatus status, @Nullable SkuType skuType,
                                 @Nullable Long categoryId, @Nullable String keyword, @Nullable String tag,
                                 boolean includeDeleted);
}
