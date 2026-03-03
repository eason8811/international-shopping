package shopping.international.domain.service.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.aggregate.customerservice.AfterSalesReship;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminReshipPageCriteria;
import shopping.international.domain.model.vo.customerservice.ReshipCreateItemCommand;

import java.util.List;

/**
 * 管理侧补发单领域服务接口
 */
public interface IAdminReshipService {

    /**
     * 基于工单创建补发单
     *
     * @param actorUserId    操作者用户 ID
     * @param ticketId       工单 ID
     * @param orderId        订单 ID
     * @param reasonCode     补发原因
     * @param currency       币种
     * @param note           备注
     * @param itemCommands   补发明细命令列表
     * @param idempotencyKey 幂等键
     * @return 补发单详情
     */
    @NotNull
    AfterSalesReship createReshipByTicket(@NotNull Long actorUserId,
                                          @NotNull Long ticketId,
                                          @NotNull Long orderId,
                                          @NotNull ReshipReasonCode reasonCode,
                                          @Nullable String currency,
                                          @Nullable String note,
                                          @NotNull List<ReshipCreateItemCommand> itemCommands,
                                          @NotNull String idempotencyKey);

    /**
     * 分页查询管理侧补发单
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 补发单分页结果
     */
    @NotNull
    PageResult<AfterSalesReship> pageReships(@NotNull AdminReshipPageCriteria criteria,
                                             @NotNull PageQuery pageQuery);

    /**
     * 查询补发单详情
     *
     * @param reshipId 补发单 ID
     * @return 补发单详情
     */
    @NotNull
    AfterSalesReship getReshipDetail(@NotNull Long reshipId);

    /**
     * 更新补发单元数据
     *
     * @param actorUserId    操作者用户 ID
     * @param reshipId       补发单 ID
     * @param currency       币种
     * @param itemsCost      货品成本, Minor 形式
     * @param shippingCost   运费成本, Minor 形式
     * @param note           备注
     * @param idempotencyKey 幂等键
     * @return 更新后的补发单详情
     */
    @NotNull
    AfterSalesReship patchReship(@NotNull Long actorUserId,
                                 @NotNull Long reshipId,
                                 @Nullable String currency,
                                 @Nullable Long itemsCost,
                                 @Nullable Long shippingCost,
                                 @Nullable String note,
                                 @NotNull String idempotencyKey);

    /**
     * 推进补发单状态
     *
     * @param actorUserId    操作者用户 ID
     * @param reshipId       补发单 ID
     * @param toStatus       目标状态
     * @param note           备注
     * @param idempotencyKey 幂等键
     * @return 更新后的补发单详情
     */
    @NotNull
    AfterSalesReship transitionReshipStatus(@NotNull Long actorUserId,
                                            @NotNull Long reshipId,
                                            @NotNull ReshipStatus toStatus,
                                            @Nullable String note,
                                            @NotNull String idempotencyKey);

    /**
     * 查询补发单关联物流单列表
     *
     * @param reshipId 补发单 ID
     * @return 关联物流单列表
     */
    @NotNull
    List<ReshipShipment> listReshipShipments(@NotNull Long reshipId);

    /**
     * 绑定补发单和物流单
     *
     * @param actorUserId    操作者用户 ID
     * @param reshipId       补发单 ID
     * @param shipmentIds    物流单 ID 列表
     * @param idempotencyKey 幂等键
     * @return 绑定后的关联物流单列表
     */
    @NotNull
    List<ReshipShipment> bindReshipShipments(@NotNull Long actorUserId,
                                             @NotNull Long reshipId,
                                             @NotNull List<Long> shipmentIds,
                                             @NotNull String idempotencyKey);
}
