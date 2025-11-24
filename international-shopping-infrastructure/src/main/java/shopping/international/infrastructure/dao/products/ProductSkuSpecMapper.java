package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductSkuSpecPO;

/**
 * Mapper: product_sku_spec
 */
@Mapper
public interface ProductSkuSpecMapper extends BaseMapper<ProductSkuSpecPO> {
}
