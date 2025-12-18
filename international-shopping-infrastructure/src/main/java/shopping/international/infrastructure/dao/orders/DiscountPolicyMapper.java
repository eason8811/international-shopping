package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.DiscountPolicyPO;

/**
 * Mapper: discount_policy
 *
 * <p>继承 {@link BaseMapper}, 提供折扣策略表的通用 CRUD 能力</p>
 */
@Mapper
public interface DiscountPolicyMapper extends BaseMapper<DiscountPolicyPO> {
}

