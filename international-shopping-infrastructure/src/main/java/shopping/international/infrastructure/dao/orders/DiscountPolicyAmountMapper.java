package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.DiscountPolicyAmountPO;

/**
 * Mapper: discount_policy_amount
 *
 * <p>用于维护折扣策略的按币种金额配置</p>
 */
@Mapper
public interface DiscountPolicyAmountMapper extends BaseMapper<DiscountPolicyAmountPO> {
}
