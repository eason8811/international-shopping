package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.OrderStatusLogPO;

/**
 * Mapper: order_status_log
 *
 * <p>继承 {@link BaseMapper}, 提供订单状态流转日志表的通用 CRUD 能力</p>
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLogPO> {
}

