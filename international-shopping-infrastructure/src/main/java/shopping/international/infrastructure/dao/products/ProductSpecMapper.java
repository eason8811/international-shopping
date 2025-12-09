package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.products.po.ProductSpecPO;

/**
 * Mapper: product_spec
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductSpecMapper extends BaseMapper<ProductSpecPO> {

    /**
     * 聚合读取商品下的规格列表, 包含规格值与多语言
     *
     * @param productId 商品 ID
     * @return 规格列表
     */
    java.util.List<ProductSpecPO> selectAggregateByProductId(@Param("productId") Long productId);

    /**
     * 按 ID 聚合读取规格, 包含规格值与多语言
     *
     * @param productId 商品 ID
     * @param specId    规格 ID
     * @return 规格聚合
     */
    ProductSpecPO selectAggregateById(@Param("productId") Long productId, @Param("specId") Long specId);
}
