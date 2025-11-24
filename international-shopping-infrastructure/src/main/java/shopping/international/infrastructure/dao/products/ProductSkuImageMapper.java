package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductSkuImagePO;

/**
 * Mapper: product_sku_image
 */
@Mapper
public interface ProductSkuImageMapper extends BaseMapper<ProductSkuImagePO> {
}
