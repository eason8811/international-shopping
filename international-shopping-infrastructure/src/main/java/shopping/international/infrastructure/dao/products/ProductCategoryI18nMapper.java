package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductCategoryI18nPO;

/**
 * Mapper: product_category_i18n
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductCategoryI18nMapper extends BaseMapper<ProductCategoryI18nPO> {
}

