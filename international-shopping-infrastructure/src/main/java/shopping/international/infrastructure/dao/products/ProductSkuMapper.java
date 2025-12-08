package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductSkuPO;

/**
 * Mapper: product_sku
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSkuPO> {

    /**
     * 聚合读取指定 SKU（含价格、规格绑定、图片）
     *
     * @param productId 商品ID
     * @param skuId     SKU ID
     * @return 聚合 PO
     */
    ProductSkuPO selectAggregateById(@Param("productId") Long productId, @Param("skuId") Long skuId);

    /**
     * 按商品聚合读取 SKU 列表
     *
     * @param productId 商品ID
     * @param status    状态过滤, 可空
     * @return SKU 聚合列表
     */
    java.util.List<ProductSkuPO> selectAggregateByProductId(@Param("productId") Long productId, @Param("status") String status);
}
