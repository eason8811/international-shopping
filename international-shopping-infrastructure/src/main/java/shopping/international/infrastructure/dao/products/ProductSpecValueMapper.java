package shopping.international.infrastructure.dao.products;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.products.po.ProductSpecValuePO;

/**
 * Mapper: product_spec_value
 * <p>基于 MyBatis-Plus 的通用 CRUD 接口</p>
 */
@Mapper
public interface ProductSpecValueMapper extends BaseMapper<ProductSpecValuePO> {
}

