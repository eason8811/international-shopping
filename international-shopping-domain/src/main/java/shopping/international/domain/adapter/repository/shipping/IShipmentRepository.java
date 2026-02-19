package shopping.international.domain.adapter.repository.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.orders.OrderNo;
import shopping.international.domain.model.vo.shipping.*;

import java.util.List;
import java.util.Optional;

/**
 * 物流领域仓储接口, 提供 shipment 聚合的查询和持久化能力
 */
public interface IShipmentRepository {

    /**
     * 查询用户侧订单关联物流单详情列表
     *
     * @param userId      用户主键
     * @param orderNo     订单号
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情列表
     */
    @NotNull
    List<Shipment> listUserOrderShipments(@NotNull Long userId,
                                                    @NotNull OrderNo orderNo,
                                                    boolean includeLogs);

    /**
     * 查询用户侧物流单详情
     *
     * @param userId     用户主键
     * @param shipmentNo 物流单号
     * @return 物流单详情, 不存在时为空
     */
    @NotNull
    Optional<Shipment> findUserShipmentDetail(@NotNull Long userId,
                                                        @NotNull ShipmentNo shipmentNo);

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
     * 管理侧按主键查询物流单详情
     *
     * @param shipmentId  物流单主键
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情, 不存在时为空
     */
    @NotNull
    Optional<Shipment> findShipmentDetailById(@NotNull Long shipmentId,
                                                        boolean includeLogs);

    /**
     * 按追踪号查询物流单详情
     *
     * @param trackingNo  追踪号
     * @param includeLogs 是否包含状态日志
     * @return 物流单详情, 不存在时为空
     */
    @NotNull
    Optional<Shipment> findShipmentDetailByTrackingNo(@NotNull String trackingNo,
                                                                boolean includeLogs);

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
     * 回填物流面单, 并记录状态日志
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
    Shipment fillLabel(@NotNull Long shipmentId,
                       @NotNull ShipmentLabel label,
                       @NotNull Integer shipFromAddressId,
                       @NotNull String idempotencyKey,
                       @NotNull String sourceRef,
                       @Nullable Long actorUserId,
                       @Nullable String note);

    /**
     * 批量发货, 每个物流单统一通过聚合 applyTrackingEvent 推进状态
     *
     * @param shipmentIds    物流单主键列表
     * @param idempotencyKey 请求幂等键
     * @param sourceRef      来源引用
     * @param note           备注
     * @param actorUserId    操作者主键
     * @return 更新后的物流单列表
     */
    @NotNull
    List<Shipment> dispatch(@NotNull List<Long> shipmentIds,
                            @NotNull String idempotencyKey,
                            @NotNull String sourceRef,
                            @NotNull String note,
                            @Nullable Long actorUserId);

    /**
     * 管理侧手工创建物流单
     *
     * @param command               手工创建命令
     * @param requestIdempotencyKey 请求幂等键
     * @param actorUserId           操作者主键
     * @return 创建后的物流单
     */
    @NotNull
    Shipment manualCreate(@NotNull ManualCreateShipmentCommand command,
                          @NotNull String requestIdempotencyKey,
                          @Nullable Long actorUserId);

    /**
     * 应用轨迹事件并持久化
     *
     * @param shipmentId 物流单主键
     * @param event      轨迹事件
     */
    void applyTrackingEvent(@NotNull Long shipmentId,
                                @NotNull ShipmentTrackingEvent event);

    /**
     * PAID 订单补建占位物流单, 用于支付链路或补偿任务
     *
     * @param orderId                订单主键
     * @param orderNo                订单号
     * @param shipmentIdempotencyKey 物流单幂等键
     * @param sourceRef              状态日志来源引用
     * @param sourceType             状态日志来源类型
     * @param note                   状态日志备注
     * @param actorUserId            操作者主键
     * @return 创建或复用后的物流单
     */
    @NotNull
    Shipment ensurePlaceholderForPaidOrder(@NotNull Long orderId,
                                           @NotNull String orderNo,
                                           @NotNull String shipmentIdempotencyKey,
                                           @NotNull String sourceRef,
                                           @NotNull ShipmentStatusEventSource sourceType,
                                           @Nullable String note,
                                           @Nullable Long actorUserId);

    /**
     * 补偿任务, 扫描并补建 PAID 且无物流单的订单
     *
     * @param limit           批次数量
     * @param sourceRefPrefix 来源引用前缀
     * @return 本批补建数量
     */
    int compensatePaidOrdersWithoutShipment(int limit,
                                            @NotNull String sourceRefPrefix);

    /**
     * 判断订单是否存在不允许改址的物流单
     *
     * @param orderId 订单主键
     * @return true 表示存在不允许改址的物流单
     */
    boolean existsAddressChangeForbiddenShipment(@NotNull Long orderId);
}
