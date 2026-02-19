package shopping.international.domain.service.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.shipping.*;

import java.util.List;

/**
 * 管理侧物流领域服务接口
 */
public interface IAdminShipmentService {

    /**
     * 管理侧分页查询物流单摘要
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @NotNull
    PageResult<ShipmentSummaryView> pageShipments(@NotNull ShipmentPageCriteria criteria,
                                                  @NotNull PageQuery pageQuery);

    /**
     * 管理侧查询物流单详情
     *
     * @param shipmentId 物流单主键
     * @return 物流单详情
     */
    @NotNull
    Shipment getShipmentDetail(@NotNull Long shipmentId);

    /**
     * 管理侧回填面单
     *
     * @param shipmentId        物流单主键
     * @param label             面单信息
     * @param shipFromAddressId 物流单寄出地址 ID (user_address.id)
     * @param idempotencyKey    请求幂等键
     * @param sourceRef         来源引用
     * @param actorUserId       操作者主键
     * @param note              备注
     * @return 更新后的物流单
     */
    @NotNull
    Shipment fillShipmentLabel(@NotNull Long shipmentId,
                               @NotNull ShipmentLabel label,
                               @NotNull Integer shipFromAddressId,
                               @NotNull String idempotencyKey,
                               @NotNull String sourceRef,
                               @Nullable Long actorUserId,
                               @Nullable String note);

    /**
     * 管理侧批量发货
     *
     * @param shipmentIds    物流单主键列表
     * @param idempotencyKey 请求幂等键
     * @param sourceRef      来源引用
     * @param note           备注
     * @param actorUserId    操作者主键
     * @return 更新后的物流单列表
     */
    @NotNull
    List<Shipment> dispatchShipments(@NotNull List<Long> shipmentIds,
                                     @NotNull String idempotencyKey,
                                     @NotNull String sourceRef,
                                     @NotNull String note,
                                     @Nullable Long actorUserId);

    /**
     * 管理侧手工创建物流单
     *
     * @param command               创建命令
     * @param requestIdempotencyKey 请求幂等键
     * @param actorUserId           操作者主键
     * @return 创建后的物流单
     */
    @NotNull
    Shipment manualCreateShipment(@NotNull ManualCreateShipmentCommand command,
                                  @NotNull String requestIdempotencyKey,
                                  @Nullable Long actorUserId);

    /**
     * 管理侧分页查询物流状态日志
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @NotNull
    PageResult<ShipmentStatusLog> pageStatusLogs(@NotNull ShipmentStatusLogPageCriteria criteria,
                                                 @NotNull PageQuery pageQuery);

    /**
     * 低频补偿任务, 补建 PAID 且无物流单的订单
     *
     * @param limit           批次数量
     * @param sourceRefPrefix 来源引用前缀
     * @return 本批补建数量
     */
    int compensatePaidOrdersWithoutShipment(int limit,
                                            @NotNull String sourceRefPrefix);
}
