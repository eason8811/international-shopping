package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductI18nPO;

/**
 * Mapper: product_i18n
 */
@Mapper
public interface ProductI18nMapper extends BaseMapper<ProductI18nPO> {
}
