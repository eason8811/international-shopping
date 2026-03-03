package shopping.international.infrastructure.dao.customerservice;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipShipmentPO;

import java.util.List;

/**
 * Mapper, aftersales_reship_shipment 表
 */
@Mapper
public interface AfterSalesReshipShipmentMapper extends BaseMapper<AfterSalesReshipShipmentPO> {

    /**
     * 批量插入补发单和物流单关联
     *
     * @param items 关联列表
     * @return 影响行数
     */
    int batchInsert(@Param("items") List<AfterSalesReshipShipmentPO> items);

    /**
     * 按物流单 ID 列表查询已绑定关联
     *
     * @param shipmentIds 物流单 ID 列表
     * @return 关联列表
     */
    List<AfterSalesReshipShipmentPO> listByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);
}
