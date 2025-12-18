package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.OrdersPO;

/**
 * Mapper: orders
 *
 * <p>继承 {@link BaseMapper}, 提供订单主表的通用 CRUD 能力</p>
 */
@Mapper
public interface OrdersMapper extends BaseMapper<OrdersPO> {
}

