package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductCategoryPO;

/**
 * Mapper: product_category
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategoryPO> {
}
