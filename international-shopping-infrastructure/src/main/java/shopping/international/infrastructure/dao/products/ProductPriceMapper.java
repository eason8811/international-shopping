package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductPricePO;

/**
 * Mapper: product_price
 */
@Mapper
public interface ProductPriceMapper extends BaseMapper<ProductPricePO> {
}
