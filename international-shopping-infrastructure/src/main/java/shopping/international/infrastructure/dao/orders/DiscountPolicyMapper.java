package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.orders.po.DiscountPolicyPO;

import java.util.List;

/**
 * Mapper: discount_policy
 *
 * <p>继承 {@link BaseMapper}, 提供折扣策略表的通用 CRUD 能力</p>
 */
@Mapper
public interface DiscountPolicyMapper extends BaseMapper<DiscountPolicyPO> {

    /**
     * 分页查询折扣策略, 并一次性聚合查询其按币种金额配置
     *
     * @param name         策略名称 (可为空)
     * @param applyScope   作用域过滤 (可为空)
     * @param strategyType 类型过滤 (可为空)
     * @param offset       偏移量
     * @param limit        单页数量
     * @return 折扣策略列表 (包含 amounts)
     */
    List<DiscountPolicyPO> pageWithAmounts(@Param("name") String name,
                                           @Param("applyScope") String applyScope,
                                           @Param("strategyType") String strategyType,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);
}
