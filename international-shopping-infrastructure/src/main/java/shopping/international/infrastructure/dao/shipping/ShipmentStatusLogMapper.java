package shopping.international.infrastructure.dao.shipping;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.shipping.po.ShipmentStatusLogPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper, shipment_status_log 表
 */
@Mapper
public interface ShipmentStatusLogMapper extends BaseMapper<ShipmentStatusLogPO> {

    /**
     * 按物流单主键集合查询状态日志
     */
    List<ShipmentStatusLogPO> selectByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    /**
     * 批量插入状态日志
     */
    int batchInsert(@Param("logs") List<ShipmentStatusLogPO> logs);

    /**
     * 分页查询状态日志
     */
    List<ShipmentStatusLogPO> pageLogs(@Param("shipmentId") Long shipmentId,
                                       @Param("orderNo") String orderNo,
                                       @Param("fromStatus") String fromStatus,
                                       @Param("toStatus") String toStatus,
                                       @Param("sourceType") String sourceType,
                                       @Param("sourceRef") String sourceRef,
                                       @Param("carrierCode") String carrierCode,
                                       @Param("trackingNo") String trackingNo,
                                       @Param("eventTimeFrom") LocalDateTime eventTimeFrom,
                                       @Param("eventTimeTo") LocalDateTime eventTimeTo,
                                       @Param("createdFrom") LocalDateTime createdFrom,
                                       @Param("createdTo") LocalDateTime createdTo,
                                       @Param("sortField") String sortField,
                                       @Param("sortDirection") String sortDirection,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 统计状态日志数量
     */
    long countLogs(@Param("shipmentId") Long shipmentId,
                   @Param("orderNo") String orderNo,
                   @Param("fromStatus") String fromStatus,
                   @Param("toStatus") String toStatus,
                   @Param("sourceType") String sourceType,
                   @Param("sourceRef") String sourceRef,
                   @Param("carrierCode") String carrierCode,
                   @Param("trackingNo") String trackingNo,
                   @Param("eventTimeFrom") LocalDateTime eventTimeFrom,
                   @Param("eventTimeTo") LocalDateTime eventTimeTo,
                   @Param("createdFrom") LocalDateTime createdFrom,
                   @Param("createdTo") LocalDateTime createdTo);
}
