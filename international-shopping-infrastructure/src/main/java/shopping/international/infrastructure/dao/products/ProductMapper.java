package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductPO;

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
}
