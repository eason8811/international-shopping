package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductImagePO;

/**
 * Mapper: product_image
 */
@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImagePO> {
}
