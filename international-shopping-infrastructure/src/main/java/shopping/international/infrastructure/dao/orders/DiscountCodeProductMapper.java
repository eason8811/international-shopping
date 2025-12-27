package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.DiscountCodeProductPO;

/**
 * Mapper: discount_code_product
 *
 * <p>继承 {@link BaseMapper}, 用于映射表的增删改查 (主要通过条件包装器操作)</p>
 */
@Mapper
public interface DiscountCodeProductMapper extends BaseMapper<DiscountCodeProductPO> {
}

