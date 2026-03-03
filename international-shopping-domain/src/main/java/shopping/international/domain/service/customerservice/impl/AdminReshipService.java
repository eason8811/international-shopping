package shopping.international.domain.service.customerservice.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.customerservice.ITicketIdempotencyPort;
import shopping.international.domain.adapter.repository.customerservice.IAdminReshipRepository;
import shopping.international.domain.adapter.repository.customerservice.IAdminTicketRepository;
import shopping.international.domain.model.aggregate.customerservice.AfterSalesReship;
import shopping.international.domain.model.aggregate.customerservice.CustomerServiceTicket;
import shopping.international.domain.model.entity.customerservice.ReshipItem;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminReshipPageCriteria;
import shopping.international.domain.model.vo.customerservice.ReshipCreateItemCommand;
import shopping.international.domain.service.customerservice.IAdminReshipService;
import shopping.international.types.exceptions.AppException;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.NotFoundException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNull;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧补发单领域服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminReshipService implements IAdminReshipService {

    /**
     * 幂等占位状态 TTL
     */
    private static final Duration IDEMPOTENCY_PENDING_TTL = Duration.ofMinutes(5);
    /**
     * 幂等成功状态 TTL
     */
    private static final Duration IDEMPOTENCY_SUCCESS_TTL = Duration.ofHours(24);

    /**
     * 管理侧创建补发单幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_RESHIP_CREATE = "admin_reship_create";
    /**
     * 管理侧更新补发单幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_RESHIP_PATCH = "admin_reship_patch";
    /**
     * 管理侧补发状态推进幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_RESHIP_STATUS = "admin_reship_status";
    /**
     * 管理侧补发单绑定物流单幂等场景
     */
    private static final String IDEMPOTENCY_SCENE_ADMIN_RESHIP_BIND_SHIPMENTS = "admin_reship_bind_shipments";

    /**
     * 管理侧补发单仓储
     */
    private final IAdminReshipRepository adminReshipRepository;
    /**
     * 管理侧工单仓储
     */
    private final IAdminTicketRepository adminTicketRepository;
    /**
     * 工单幂等端口
     */
    private final ITicketIdempotencyPort ticketIdempotencyPort;

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
    @Override
    public @NotNull AfterSalesReship createReshipByTicket(@NotNull Long actorUserId,
                                                          @NotNull Long ticketId,
                                                          @NotNull Long orderId,
                                                          @NotNull ReshipReasonCode reasonCode,
                                                          @Nullable String currency,
                                                          @Nullable String note,
                                                          @NotNull List<ReshipCreateItemCommand> itemCommands,
                                                          @NotNull String idempotencyKey) {
        require(!itemCommands.isEmpty(), "itemCommands 不能为空");
        itemCommands.forEach(ReshipCreateItemCommand::validate);

        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_RESHIP_CREATE,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return loadReshipByTokenResultRef(tokenStatus.ticketNo());
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的补发单创建请求正在处理中");

        CustomerServiceTicket ticket = adminTicketRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new NotFoundException("工单不存在"));
        Long ticketOrderId = normalizeNotNull(ticket.getOrderId(), "ticket.orderId 不能为空");
        if (!Objects.equals(ticketOrderId, orderId))
            throw new ConflictException("orderId 与工单关联订单不一致");
        Long originalShipmentId = normalizeNotNull(ticket.getShipmentId(), "工单未关联原物流单");

        List<Long> orderItemIdList = itemCommands.stream()
                .map(ReshipCreateItemCommand::orderItemId)
                .distinct()
                .toList();
        require(orderItemIdList.size() == itemCommands.size(), "同一补发单内 orderItemId 不允许重复");

        List<IAdminReshipRepository.OrderItemSnapshot> snapshotList = adminReshipRepository.listOrderItemSnapshots(orderId, orderItemIdList);
        if (snapshotList.size() != orderItemIdList.size())
            throw new NotFoundException("存在不属于当前订单的 orderItemId");
        Map<Long, IAdminReshipRepository.OrderItemSnapshot> orderItemSnapshotMap = toOrderItemSnapshotMap(snapshotList);

        List<ReshipItem> itemList = itemCommands.stream()
                .map(command -> toReshipItem(command, orderItemSnapshotMap))
                .toList();

        String effectiveCurrency = currency;
        if (effectiveCurrency == null || effectiveCurrency.isBlank())
            effectiveCurrency = ticket.getCurrency();

        AfterSalesReship created = AfterSalesReship.create(
                orderId,
                ticketId,
                originalShipmentId,
                reasonCode,
                effectiveCurrency,
                note,
                itemList
        );
        AfterSalesReship persisted = adminReshipRepository.saveReshipWithItems(created);
        Long reshipId = normalizeNotNull(persisted.getId(), "reshipId 不能为空");
        ticketIdempotencyPort.markActionSucceeded(
                IDEMPOTENCY_SCENE_ADMIN_RESHIP_CREATE,
                actorUserId,
                String.valueOf(ticketId),
                idempotencyKey,
                String.valueOf(reshipId),
                IDEMPOTENCY_SUCCESS_TTL
        );
        return persisted;
    }

    /**
     * 分页查询管理侧补发单
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 补发单分页结果
     */
    @Override
    public @NotNull PageResult<AfterSalesReship> pageReships(@NotNull AdminReshipPageCriteria criteria,
                                                             @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();
        return adminReshipRepository.pageAdminReships(criteria, pageQuery);
    }

    /**
     * 查询补发单详情
     *
     * @param reshipId 补发单 ID
     * @return 补发单详情
     */
    @Override
    public @NotNull AfterSalesReship getReshipDetail(@NotNull Long reshipId) {
        return requireReship(reshipId, true);
    }

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
    @Override
    public @NotNull AfterSalesReship patchReship(@NotNull Long actorUserId,
                                                 @NotNull Long reshipId,
                                                 @Nullable String currency,
                                                 @Nullable Long itemsCost,
                                                 @Nullable Long shippingCost,
                                                 @Nullable String note,
                                                 @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_RESHIP_PATCH,
                actorUserId,
                String.valueOf(reshipId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return requireReship(reshipId, true);
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的补发单更新请求正在处理中");

        AfterSalesReship reship = requireReship(reshipId, false);
        LocalDateTime expectedUpdatedAt = normalizeNotNull(reship.getUpdatedAt(), "updatedAt 不能为空");
        reship.patch(currency, itemsCost, shippingCost, note);

        boolean updated = adminReshipRepository.updateReshipMetadataWithCas(reship, expectedUpdatedAt);
        if (!updated) {
            AfterSalesReship latest = requireReship(reshipId, false);
            if (metadataAlreadyApplied(latest, currency, itemsCost, shippingCost, note)) {
                markSucceeded(IDEMPOTENCY_SCENE_ADMIN_RESHIP_PATCH, actorUserId, reshipId, idempotencyKey);
                return requireReship(reshipId, true);
            }
            throw new ConflictException("补发单已被其他请求更新, 请刷新后重试");
        }

        AfterSalesReship detail = requireReship(reshipId, true);
        markSucceeded(IDEMPOTENCY_SCENE_ADMIN_RESHIP_PATCH, actorUserId, reshipId, idempotencyKey);
        return detail;
    }

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
    @Override
    public @NotNull AfterSalesReship transitionReshipStatus(@NotNull Long actorUserId,
                                                            @NotNull Long reshipId,
                                                            @NotNull ReshipStatus toStatus,
                                                            @Nullable String note,
                                                            @NotNull String idempotencyKey) {
        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_RESHIP_STATUS,
                actorUserId,
                String.valueOf(reshipId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return requireReship(reshipId, true);
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的补发状态推进请求正在处理中");

        AfterSalesReship reship = requireReship(reshipId, false);
        ReshipStatus expectedFromStatus = reship.getStatus();
        if (note != null)
            reship.patch(null, null, null, note);
        reship.transitionStatus(toStatus);

        boolean updated = adminReshipRepository.updateReshipStatusWithCas(reship, expectedFromStatus);
        if (!updated) {
            AfterSalesReship latest = requireReship(reshipId, false);
            if (latest.getStatus() == toStatus && statusPatchNoteAlreadyApplied(latest, note)) {
                markSucceeded(IDEMPOTENCY_SCENE_ADMIN_RESHIP_STATUS, actorUserId, reshipId, idempotencyKey);
                return requireReship(reshipId, true);
            }
            throw new ConflictException("补发状态已变化, 请刷新后重试");
        }

        AfterSalesReship detail = requireReship(reshipId, true);
        markSucceeded(IDEMPOTENCY_SCENE_ADMIN_RESHIP_STATUS, actorUserId, reshipId, idempotencyKey);
        return detail;
    }

    /**
     * 查询补发单关联物流单列表
     *
     * @param reshipId 补发单 ID
     * @return 关联物流单列表
     */
    @Override
    public @NotNull List<ReshipShipment> listReshipShipments(@NotNull Long reshipId) {
        requireReship(reshipId, false);
        return adminReshipRepository.listReshipShipments(reshipId);
    }

    /**
     * 绑定补发单和物流单
     *
     * @param actorUserId    操作者用户 ID
     * @param reshipId       补发单 ID
     * @param shipmentIds    物流单 ID 列表
     * @param idempotencyKey 幂等键
     * @return 绑定后的关联物流单列表
     */
    @Override
    public @NotNull List<ReshipShipment> bindReshipShipments(@NotNull Long actorUserId,
                                                             @NotNull Long reshipId,
                                                             @NotNull List<Long> shipmentIds,
                                                             @NotNull String idempotencyKey) {
        require(!shipmentIds.isEmpty(), "shipmentIds 不能为空");
        List<Long> dedupShipmentIds = shipmentIds.stream().distinct().toList();

        ITicketIdempotencyPort.TokenStatus tokenStatus = ticketIdempotencyPort.registerActionOrGet(
                IDEMPOTENCY_SCENE_ADMIN_RESHIP_BIND_SHIPMENTS,
                actorUserId,
                String.valueOf(reshipId),
                idempotencyKey,
                IDEMPOTENCY_PENDING_TTL
        );
        if (tokenStatus.isSucceeded())
            return listReshipShipments(reshipId);
        if (tokenStatus.status() == ITicketIdempotencyPort.TokenStatus.Status.IN_PROGRESS)
            throw new ConflictException("相同幂等键的补发绑定请求正在处理中");

        AfterSalesReship reship = requireReship(reshipId, false);
        validateShipmentOrders(reship, dedupShipmentIds);

        Map<Long, Long> boundMap = adminReshipRepository.mapBoundReshipByShipmentIds(dedupShipmentIds);
        for (Map.Entry<Long, Long> entry : boundMap.entrySet()) {
            Long boundReshipId = entry.getValue();
            if (!Objects.equals(boundReshipId, reshipId))
                throw new ConflictException("物流单 " + entry.getKey() + " 已绑定到其他补发单");
        }

        List<Long> missingShipmentIds = dedupShipmentIds.stream()
                .filter(shipmentId -> !boundMap.containsKey(shipmentId))
                .toList();
        if (!missingShipmentIds.isEmpty()) {
            int updateRows = 0;
            try {
                updateRows = adminReshipRepository.bindReshipShipments(reshipId, missingShipmentIds);
            } catch (Exception ignored) {
                // 并发绑定下, 通过回查结果判定是否已达到目标状态,
            }
            if (updateRows != missingShipmentIds.size()) {
                Map<Long, Long> latestBoundMap = adminReshipRepository.mapBoundReshipByShipmentIds(dedupShipmentIds);
                for (Long shipmentId : dedupShipmentIds) {
                    Long latestReshipId = latestBoundMap.get(shipmentId);
                    if (!Objects.equals(latestReshipId, reshipId))
                        throw new ConflictException("物流单 " + shipmentId + " 已绑定到其他补发单");
                }
            }
        }

        List<ReshipShipment> shipmentList = adminReshipRepository.listReshipShipments(reshipId);
        markSucceeded(IDEMPOTENCY_SCENE_ADMIN_RESHIP_BIND_SHIPMENTS, actorUserId, reshipId, idempotencyKey);
        return shipmentList;
    }

    /**
     * 校验物流单归属订单与补发单是否一致
     *
     * @param reship      补发单聚合
     * @param shipmentIds 物流单 ID 列表
     */
    private void validateShipmentOrders(@NotNull AfterSalesReship reship,
                                        @NotNull List<Long> shipmentIds) {
        List<IAdminReshipRepository.ShipmentOrderSnapshot> shipmentSnapshotList = adminReshipRepository.listShipmentOrderSnapshots(shipmentIds);
        if (shipmentSnapshotList.size() != shipmentIds.size())
            throw new NotFoundException("存在不存在的物流单");

        Long orderId = reship.getOrderId();
        for (IAdminReshipRepository.ShipmentOrderSnapshot snapshot : shipmentSnapshotList)
            if (!Objects.equals(snapshot.orderId(), orderId))
                throw new ConflictException("物流单 " + snapshot.shipmentId() + " 与补发单订单不一致");
    }

    /**
     * 查询补发单聚合, 不存在时抛出异常
     *
     * @param reshipId       补发单 ID
     * @param includeDetails 是否包含明细
     * @return 补发单聚合
     */
    private @NotNull AfterSalesReship requireReship(@NotNull Long reshipId, boolean includeDetails) {
        return adminReshipRepository.findByReshipId(reshipId, includeDetails)
                .orElseThrow(() -> new NotFoundException("补发单不存在"));
    }

    /**
     * 幂等命中后根据结果引用加载补发单详情
     *
     * @param resultRef 结果引用
     * @return 补发单详情
     */
    private @NotNull AfterSalesReship loadReshipByTokenResultRef(@Nullable String resultRef) {
        if (resultRef == null || resultRef.isBlank())
            throw new AppException("创建补发单, 幂等结果缺少 resultRef");
        try {
            Long reshipId = Long.parseLong(resultRef);
            return requireReship(reshipId, true);
        } catch (NumberFormatException exception) {
            throw new AppException("创建补发单, 幂等结果引用格式不合法");
        }
    }

    /**
     * 订单明细快照列表转换为映射
     *
     * @param snapshotList 订单明细快照列表
     * @return orderItemId -> 快照映射
     */
    private @NotNull Map<Long, IAdminReshipRepository.OrderItemSnapshot> toOrderItemSnapshotMap(@NotNull List<IAdminReshipRepository.OrderItemSnapshot> snapshotList) {
        Map<Long, IAdminReshipRepository.OrderItemSnapshot> snapshotMap = new LinkedHashMap<>(snapshotList.size());
        for (IAdminReshipRepository.OrderItemSnapshot snapshot : snapshotList)
            snapshotMap.put(snapshot.orderItemId(), snapshot);
        return snapshotMap;
    }

    /**
     * 创建补发明细实体
     *
     * @param command          创建命令
     * @param orderItemMapById 订单明细快照映射
     * @return 补发明细实体
     */
    private @NotNull ReshipItem toReshipItem(@NotNull ReshipCreateItemCommand command,
                                             @NotNull Map<Long, IAdminReshipRepository.OrderItemSnapshot> orderItemMapById) {
        IAdminReshipRepository.OrderItemSnapshot snapshot = orderItemMapById.get(command.orderItemId());
        if (snapshot == null)
            throw new NotFoundException("orderItemId 不存在: " + command.orderItemId());
        if (!Objects.equals(snapshot.skuId(), command.skuId()))
            throw new ConflictException("orderItemId 与 skuId 不匹配");
        if (command.quantity() > snapshot.quantity())
            throw new ConflictException("补发数量不能大于原订单明细数量");

        long amount;
        try {
            amount = Math.multiplyExact(snapshot.unitPrice(), command.quantity().longValue());
        } catch (ArithmeticException exception) {
            throw new ConflictException("补发明细金额超出范围");
        }
        return ReshipItem.create(null, command.orderItemId(), command.skuId(), command.quantity(), amount);
    }

    /**
     * 判断元数据更新是否已生效
     *
     * @param latest       最新补发单聚合
     * @param currency     币种
     * @param itemsCost    货品成本, Minor 形式
     * @param shippingCost 运费成本, Minor 形式
     * @param note         备注
     * @return 是否已生效
     */
    private boolean metadataAlreadyApplied(@NotNull AfterSalesReship latest,
                                           @Nullable String currency,
                                           @Nullable Long itemsCost,
                                           @Nullable Long shippingCost,
                                           @Nullable String note) {
        if (currency != null && !Objects.equals(latest.getCurrency(), currency.strip().toUpperCase()))
            return false;
        if (itemsCost != null && !Objects.equals(latest.getItemsCost(), itemsCost))
            return false;
        if (shippingCost != null && !Objects.equals(latest.getShippingCost(), shippingCost))
            return false;
        if (note != null)
            return Objects.equals(latest.getNote(), note.strip());
        return true;
    }

    /**
     * 判断状态推进附带备注是否已生效
     *
     * @param latest 最新补发单聚合
     * @param note   备注
     * @return 是否已生效
     */
    private boolean statusPatchNoteAlreadyApplied(@NotNull AfterSalesReship latest,
                                                  @Nullable String note) {
        if (note == null)
            return true;
        return Objects.equals(latest.getNote(), note.strip());
    }

    /**
     * 标记补发写操作幂等成功
     *
     * @param scene          幂等场景
     * @param actorUserId    操作者用户 ID
     * @param reshipId       补发单 ID
     * @param idempotencyKey 幂等键
     */
    private void markSucceeded(@NotNull String scene,
                               @NotNull Long actorUserId,
                               @NotNull Long reshipId,
                               @NotNull String idempotencyKey) {
        ticketIdempotencyPort.markActionSucceeded(
                scene,
                actorUserId,
                String.valueOf(reshipId),
                idempotencyKey,
                String.valueOf(reshipId),
                IDEMPOTENCY_SUCCESS_TTL
        );
    }
}
