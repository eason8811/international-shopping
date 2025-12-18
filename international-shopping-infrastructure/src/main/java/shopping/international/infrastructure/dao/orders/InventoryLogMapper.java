package shopping.international.infrastructure.dao.orders;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import shopping.international.infrastructure.dao.orders.po.InventoryLogPO;

/**
 * Mapper: inventory_log
 *
 * <p>继承 {@link BaseMapper}, 提供库存变动日志表的通用 CRUD 能力</p>
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLogPO> {
}

