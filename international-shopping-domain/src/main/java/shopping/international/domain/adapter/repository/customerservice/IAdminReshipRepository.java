package shopping.international.domain.adapter.repository.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.customerservice.AfterSalesReship;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminReshipPageCriteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理侧补发单仓储接口
 */
public interface IAdminReshipRepository {

    /**
     * 订单明细快照
     *
     * @param orderItemId 原订单明细 ID
     * @param orderId     订单 ID
     * @param skuId       SKU ID
     * @param quantity    原订单明细数量
     * @param unitPrice   原订单明细单价, Minor 形式
     */
    record OrderItemSnapshot(@NotNull Long orderItemId,
                             @NotNull Long orderId,
                             @NotNull Long skuId,
                             @NotNull Integer quantity,
                             @NotNull Long unitPrice) {
    }

    /**
     * 物流单归属快照
     *
     * @param shipmentId 物流单 ID
     * @param orderId    订单 ID
     */
    record ShipmentOrderSnapshot(@NotNull Long shipmentId,
                                 @Nullable Long orderId) {
    }

    /**
     * 分页查询管理侧补发单
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 补发单分页结果
     */
    @NotNull
    PageResult<AfterSalesReship> pageAdminReships(@NotNull AdminReshipPageCriteria criteria,
                                                  @NotNull PageQuery pageQuery);

    /**
     * 按补发单 ID 查询补发单聚合
     *
     * @param reshipId       补发单 ID
     * @param includeDetails 是否加载明细和关联物流单
     * @return 补发单聚合
     */
    @NotNull
    Optional<AfterSalesReship> findByReshipId(@NotNull Long reshipId,
                                              boolean includeDetails);

    /**
     * 保存补发单和明细
     *
     * @param reship 补发单聚合
     * @return 落库后的补发单聚合
     */
    @NotNull
    AfterSalesReship saveReshipWithItems(@NotNull AfterSalesReship reship);

    /**
     * 基于更新时间 CAS 更新补发单元数据
     *
     * @param reship             补发单聚合
     * @param expectedUpdatedAt 期望旧更新时间
     * @return 更新是否成功,
     */
    boolean updateReshipMetadataWithCas(@NotNull AfterSalesReship reship,
                                        @NotNull LocalDateTime expectedUpdatedAt);

    /**
     * 基于状态 CAS 推进补发单状态
     *
     * @param reship             补发单聚合
     * @param expectedFromStatus 期望旧状态
     * @return 更新是否成功
     */
    boolean updateReshipStatusWithCas(@NotNull AfterSalesReship reship,
                                      @NotNull ReshipStatus expectedFromStatus);

    /**
     * 查询补发单关联物流单列表
     *
     * @param reshipId 补发单 ID
     * @return 关联物流单列表
     */
    @NotNull
    List<ReshipShipment> listReshipShipments(@NotNull Long reshipId);

    /**
     * 按订单和订单明细 ID 列表查询订单明细快照
     *
     * @param orderId      订单 ID
     * @param orderItemIds 订单明细 ID 列表
     * @return 订单明细快照列表
     */
    @NotNull
    List<OrderItemSnapshot> listOrderItemSnapshots(@NotNull Long orderId,
                                                   @NotNull List<Long> orderItemIds);

    /**
     * 按物流单 ID 列表查询物流单归属快照
     *
     * @param shipmentIds 物流单 ID 列表
     * @return 物流单归属快照列表
     */
    @NotNull
    List<ShipmentOrderSnapshot> listShipmentOrderSnapshots(@NotNull List<Long> shipmentIds);

    /**
     * 按物流单 ID 列表查询已绑定补发单映射
     *
     * @param shipmentIds 物流单 ID 列表
     * @return shipmentId -> reshipId 映射
     */
    @NotNull
    Map<Long, Long> mapBoundReshipByShipmentIds(@NotNull List<Long> shipmentIds);

    /**
     * 批量绑定补发单和物流单
     *
     * @param reshipId     补发单 ID
     * @param shipmentIds  物流单 ID 列表
     * @return 影响行数
     */
    int bindReshipShipments(@NotNull Long reshipId,
                            @NotNull List<Long> shipmentIds);
}
