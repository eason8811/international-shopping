package shopping.international.infrastructure.dao.shipping;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.shipping.po.ShipmentItemPO;

import java.util.List;

/**
 * Mapper, shipment_item 表
 */
@Mapper
public interface ShipmentItemMapper extends BaseMapper<ShipmentItemPO> {

    /**
     * 按物流单主键集合查询物流明细
     */
    List<ShipmentItemPO> selectByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    /**
     * 批量插入物流明细
     */
    int batchInsert(@Param("items") List<ShipmentItemPO> items);
}
