package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductSkuPO;

/**
 * Mapper: product_sku
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSkuPO> {
}
