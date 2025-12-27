package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.DiscountCodePO;

/**
 * Mapper: discount_code
 *
 * <p>继承 {@link BaseMapper}, 提供折扣码表的通用 CRUD 能力</p>
 */
@Mapper
public interface DiscountCodeMapper extends BaseMapper<DiscountCodePO> {
}

