package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.OrderItemPO;

/**
 * Mapper: order_item
 *
 * <p>继承 {@link BaseMapper}, 提供订单明细表的通用 CRUD 能力</p>
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemPO> {
}

