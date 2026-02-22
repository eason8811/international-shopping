package shopping.international.infrastructure.dao.shipping;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import shopping.international.infrastructure.dao.shipping.po.PaidOrderCandidatePO;
import shopping.international.infrastructure.dao.shipping.po.ShipmentDispatchStatusCasPO;
import shopping.international.infrastructure.dao.shipping.po.ShipmentPO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper, shipment 表
 */
@Mapper
public interface ShipmentMapper extends BaseMapper<ShipmentPO> {

    /**
     * 管理侧分页查询物流单摘要
     *
     * @param shipmentNo    物流单号
     * @param orderNo       订单号
     * @param orderId       订单 ID
     * @param carrierCode   承运商代码
     * @param trackingNo    追踪号
     * @param extExternalId 外部系统 ID
     * @param statusIn      物流状态列表
     * @param updatedFrom   更新时间起始
     * @param updatedTo     更新时间结束
     * @param createdFrom   创建时间起始
     * @param createdTo     创建时间结束
     * @param sortField     排序字段
     * @param sortDirection 排序方向 (如 ASC, DESC)
     * @param offset        分页偏移量
     * @param limit         每页记录数
     * @return 符合条件的物流单列表
     */
    List<ShipmentPO> pageAdminShipments(@Param("shipmentNo") String shipmentNo,
                                        @Param("orderNo") String orderNo,
                                        @Param("orderId") Long orderId,
                                        @Param("carrierCode") String carrierCode,
                                        @Param("trackingNo") String trackingNo,
                                        @Param("extExternalId") String extExternalId,
                                        @Param("statusIn") List<String> statusIn,
                                        @Param("updatedFrom") LocalDateTime updatedFrom,
                                        @Param("updatedTo") LocalDateTime updatedTo,
                                        @Param("createdFrom") LocalDateTime createdFrom,
                                        @Param("createdTo") LocalDateTime createdTo,
                                        @Param("sortField") String sortField,
                                        @Param("sortDirection") String sortDirection,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 管理侧统计物流单数量
     *
     * @param shipmentNo    物流单号
     * @param orderNo       订单号
     * @param orderId       订单 ID
     * @param carrierCode   承运商代码
     * @param trackingNo    追踪号
     * @param extExternalId 外部系统 ID
     * @param statusIn      物流状态列表
     * @param updatedFrom   更新时间起始
     * @param updatedTo     更新时间结束
     * @param createdFrom   创建时间起始
     * @param createdTo     创建时间结束
     * @return 符合条件的物流单总数
     */
    long countAdminShipments(@Param("shipmentNo") String shipmentNo,
                             @Param("orderNo") String orderNo,
                             @Param("orderId") Long orderId,
                             @Param("carrierCode") String carrierCode,
                             @Param("trackingNo") String trackingNo,
                             @Param("extExternalId") String extExternalId,
                             @Param("statusIn") List<String> statusIn,
                             @Param("updatedFrom") LocalDateTime updatedFrom,
                             @Param("updatedTo") LocalDateTime updatedTo,
                             @Param("createdFrom") LocalDateTime createdFrom,
                             @Param("createdTo") LocalDateTime createdTo);

    /**
     * 用户侧按订单号查询物流单详情, 联表加载 shipment_item
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 返回一个 ShipmentPO 对象列表, 包含了与指定用户和订单相关的物流单及其商品项详情
     */
    List<ShipmentPO> selectUserShipmentDetailsWithItemsByOrderNo(@Param("userId") Long userId,
                                                                 @Param("orderNo") String orderNo);

    /**
     * 用户侧按订单号查询物流单详情, 联表加载 shipment_item 和 shipment_status_log
     *
     * @param userId  用户 ID
     * @param orderNo 订单号
     * @return 返回一个 ShipmentPO 对象列表, 包含了与指定用户和订单相关的物流单及其商品项和状态日志详情
     */
    List<ShipmentPO> selectUserShipmentDetailsWithItemsAndLogsByOrderNo(@Param("userId") Long userId,
                                                                        @Param("orderNo") String orderNo);

    /**
     * 用户侧按物流单号查询物流单详情, 联表加载 shipment_item 和 shipment_status_log
     *
     * @param userId     用户的唯一标识符
     * @param shipmentNo 物流单的编号
     * @return 返回一个 ShipmentPO 对象, 包含了与指定用户和物流单号相关的物流详情及其商品项和状态日志
     */
    ShipmentPO selectUserShipmentDetailWithItemsAndLogsByNo(@Param("userId") Long userId,
                                                            @Param("shipmentNo") String shipmentNo);

    /**
     * 按主键查询物流单详情基础行
     */
    ShipmentPO selectDetailById(@Param("shipmentId") Long shipmentId);

    /**
     * 按追踪号查询物流单详情基础行
     */
    ShipmentPO selectDetailByTrackingNo(@Param("trackingNo") String trackingNo);

    /**
     * 按物流单主键集合查询详情, 联表加载 shipment_item
     */
    List<ShipmentPO> selectDetailWithItemsByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    /**
     * 按物流单主键集合查询详情, 联表加载 shipment_item 和 shipment_status_log
     */
    List<ShipmentPO> selectDetailWithItemsAndLogsByShipmentIds(@Param("shipmentIds") List<Long> shipmentIds);

    /**
     * 扫描 PAID 且无物流单的订单
     */
    List<PaidOrderCandidatePO> listPaidOrdersWithoutShipment(@Param("limit") int limit);

    /**
     * 统计订单关联物流单总数
     */
    long countByOrderId(@Param("orderId") Long orderId);

    /**
     * 统计订单关联且非 DELIVERED 的物流单数量
     */
    long countNonDeliveredByOrderId(@Param("orderId") Long orderId);

    /**
     * 当订单关联物流单全部签收时, 尝试推进订单为 FULFILLED
     */
    int tryAdvanceOrderToFulfilled(@Param("orderId") Long orderId);

    /**
     * 批量插入占位物流单, 使用 INSERT IGNORE 避免并发冲突
     */
    int batchInsertIgnore(@Param("items") List<ShipmentPO> items);

    /**
     * 批量按旧状态 CAS 更新物流单状态
     */
    int batchUpdateStatusWithCas(@Param("items") List<ShipmentDispatchStatusCasPO> items);

    /**
     * 批量按旧状态 CAS 更新物流单状态, 且要求命中状态日志闸门
     */
    int batchUpdateStatusWithCasAndGate(@Param("items") List<ShipmentDispatchStatusCasPO> items);
}
