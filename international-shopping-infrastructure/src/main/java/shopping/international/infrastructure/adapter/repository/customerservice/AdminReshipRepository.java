package shopping.international.infrastructure.adapter.repository.customerservice;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import shopping.international.domain.adapter.repository.customerservice.IAdminReshipRepository;
import shopping.international.domain.model.aggregate.customerservice.AfterSalesReship;
import shopping.international.domain.model.entity.customerservice.ReshipItem;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.domain.model.vo.PageQuery;
import shopping.international.domain.model.vo.PageResult;
import shopping.international.domain.model.vo.customerservice.AdminReshipPageCriteria;
import shopping.international.domain.model.vo.customerservice.ReshipNo;
import shopping.international.infrastructure.dao.customerservice.AfterSalesReshipItemMapper;
import shopping.international.infrastructure.dao.customerservice.AfterSalesReshipMapper;
import shopping.international.infrastructure.dao.customerservice.AfterSalesReshipShipmentMapper;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipItemPO;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipPO;
import shopping.international.infrastructure.dao.customerservice.po.AfterSalesReshipShipmentPO;
import shopping.international.infrastructure.dao.orders.OrderItemMapper;
import shopping.international.infrastructure.dao.orders.po.OrderItemPO;
import shopping.international.infrastructure.dao.shipping.ShipmentMapper;
import shopping.international.infrastructure.dao.shipping.po.ShipmentPO;
import shopping.international.types.exceptions.AppException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧补发单仓储实现, 基于 MyBatis 和 MySQL
 */
@Repository
@RequiredArgsConstructor
public class AdminReshipRepository implements IAdminReshipRepository {

    /**
     * 补发单主表 Mapper
     */
    private final AfterSalesReshipMapper afterSalesReshipMapper;
    /**
     * 补发明细表 Mapper
     */
    private final AfterSalesReshipItemMapper afterSalesReshipItemMapper;
    /**
     * 补发和物流单关联表 Mapper
     */
    private final AfterSalesReshipShipmentMapper afterSalesReshipShipmentMapper;
    /**
     * 订单明细表 Mapper
     */
    private final OrderItemMapper orderItemMapper;
    /**
     * 物流单表 Mapper
     */
    private final ShipmentMapper shipmentMapper;

    /**
     * 分页查询管理侧补发单
     *
     * @param criteria  查询条件
     * @param pageQuery 分页参数
     * @return 补发单分页结果
     */
    @Override
    public @NotNull PageResult<AfterSalesReship> pageAdminReships(@NotNull AdminReshipPageCriteria criteria,
                                                                  @NotNull PageQuery pageQuery) {
        criteria.validate();
        pageQuery.validate();

        String status = criteria.getStatus() == null ? null : criteria.getStatus().name();
        String reasonCode = criteria.getReasonCode() == null ? null : criteria.getReasonCode().name();
        List<AfterSalesReshipPO> rowList = afterSalesReshipMapper.pageAdminReships(
                criteria.getReshipNo(),
                criteria.getOrderId(),
                criteria.getTicketId(),
                status,
                reasonCode,
                criteria.getCreatedFrom(),
                criteria.getCreatedTo(),
                pageQuery.offset(),
                pageQuery.limit()
        );
        long total = afterSalesReshipMapper.countAdminReships(
                criteria.getReshipNo(),
                criteria.getOrderId(),
                criteria.getTicketId(),
                status,
                reasonCode,
                criteria.getCreatedFrom(),
                criteria.getCreatedTo()
        );

        if (rowList == null || rowList.isEmpty())
            return PageResult.<AfterSalesReship>builder()
                    .items(List.of())
                    .total(total)
                    .build();

        List<AfterSalesReship> itemList = rowList.stream()
                .map(row -> toReshipAggregate(row, List.of(), List.of()))
                .toList();
        return PageResult.<AfterSalesReship>builder()
                .items(itemList)
                .total(total)
                .build();
    }

    /**
     * 按补发单 ID 查询补发单聚合
     *
     * @param reshipId       补发单 ID
     * @param includeDetails 是否加载明细和关联物流单
     * @return 补发单聚合
     */
    @Override
    public @NotNull Optional<AfterSalesReship> findByReshipId(@NotNull Long reshipId,
                                                              boolean includeDetails) {
        AfterSalesReshipPO row = afterSalesReshipMapper.selectById(reshipId);
        if (row == null)
            return Optional.empty();
        if (!includeDetails)
            return Optional.of(toReshipAggregate(row, List.of(), List.of()));

        List<AfterSalesReshipItemPO> itemRowList = afterSalesReshipMapper.listReshipItemDetailsByReshipIds(List.of(reshipId));
        List<AfterSalesReshipShipmentPO> shipmentRowList = afterSalesReshipMapper.listReshipShipmentsByReshipIds(List.of(reshipId));
        List<ReshipItem> itemList = itemRowList == null ? List.of() : itemRowList.stream()
                .map(this::toReshipItemEntity)
                .toList();
        List<ReshipShipment> shipmentList = shipmentRowList == null ? List.of() : shipmentRowList.stream()
                .map(this::toReshipShipmentEntity)
                .toList();
        return Optional.of(toReshipAggregate(row, itemList, shipmentList));
    }

    /**
     * 保存补发单和明细
     *
     * @param reship 补发单聚合
     * @return 落库后的补发单聚合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NotNull AfterSalesReship saveReshipWithItems(@NotNull AfterSalesReship reship) {
        AfterSalesReshipPO insertRow = toInsertReshipPO(reship);
        int affectedRowCount = afterSalesReshipMapper.insert(insertRow);
        require(affectedRowCount == 1, "写入补发单失败");

        Long reshipId = insertRow.getId();
        if (reshipId == null)
            throw new AppException("写入补发单后未返回主键");

        List<ReshipItem> itemList = reship.getItemList();
        if (!itemList.isEmpty()) {
            List<AfterSalesReshipItemPO> insertItemRows = itemList.stream()
                    .map(item -> toInsertReshipItemPO(reshipId, item))
                    .toList();
            int itemAffected = afterSalesReshipItemMapper.batchInsert(insertItemRows);
            require(itemAffected == insertItemRows.size(), "写入补发明细失败");
        }

        return findByReshipId(reshipId, true)
                .orElseThrow(() -> new AppException("写入补发单后回读失败"));
    }

    /**
     * 基于更新时间 CAS 更新补发单元数据
     *
     * @param reship             补发单聚合
     * @param expectedUpdatedAt 期望旧更新时间
     * @return 更新是否成功
     */
    @Override
    public boolean updateReshipMetadataWithCas(@NotNull AfterSalesReship reship,
                                               @NotNull LocalDateTime expectedUpdatedAt) {
        Long reshipId = reship.getId();
        if (reshipId == null)
            throw new AppException("补发单未持久化, 无法更新元数据");

        LambdaUpdateWrapper<AfterSalesReshipPO> updateWrapper = new LambdaUpdateWrapper<AfterSalesReshipPO>()
                .eq(AfterSalesReshipPO::getId, reshipId)
                .eq(AfterSalesReshipPO::getUpdatedAt, expectedUpdatedAt)
                .set(AfterSalesReshipPO::getCurrency, reship.getCurrency())
                .set(AfterSalesReshipPO::getItemsCost, reship.getItemsCost())
                .set(AfterSalesReshipPO::getShippingCost, reship.getShippingCost())
                .set(AfterSalesReshipPO::getNote, reship.getNote())
                .set(AfterSalesReshipPO::getUpdatedAt, reship.getUpdatedAt());
        int affectedRowCount = afterSalesReshipMapper.update(null, updateWrapper);
        return affectedRowCount > 0;
    }

    /**
     * 基于状态 CAS 推进补发单状态
     *
     * @param reship             补发单聚合
     * @param expectedFromStatus 期望旧状态
     * @return 更新是否成功
     */
    @Override
    public boolean updateReshipStatusWithCas(@NotNull AfterSalesReship reship,
                                             @NotNull ReshipStatus expectedFromStatus) {
        Long reshipId = reship.getId();
        if (reshipId == null)
            throw new AppException("补发单未持久化, 无法推进状态");

        LambdaUpdateWrapper<AfterSalesReshipPO> updateWrapper = new LambdaUpdateWrapper<AfterSalesReshipPO>()
                .eq(AfterSalesReshipPO::getId, reshipId)
                .eq(AfterSalesReshipPO::getStatus, expectedFromStatus.name())
                .set(AfterSalesReshipPO::getStatus, reship.getStatus().name())
                .set(AfterSalesReshipPO::getNote, reship.getNote())
                .set(AfterSalesReshipPO::getUpdatedAt, reship.getUpdatedAt());
        int affectedRowCount = afterSalesReshipMapper.update(null, updateWrapper);
        return affectedRowCount > 0;
    }

    /**
     * 查询补发单关联物流单列表
     *
     * @param reshipId 补发单 ID
     * @return 关联物流单列表
     */
    @Override
    public @NotNull List<ReshipShipment> listReshipShipments(@NotNull Long reshipId) {
        List<AfterSalesReshipShipmentPO> rowList = afterSalesReshipMapper.listReshipShipmentsByReshipIds(List.of(reshipId));
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .map(this::toReshipShipmentEntity)
                .toList();
    }

    /**
     * 按订单和订单明细 ID 列表查询订单明细快照
     *
     * @param orderId      订单 ID
     * @param orderItemIds 订单明细 ID 列表
     * @return 订单明细快照列表
     */
    @Override
    public @NotNull List<OrderItemSnapshot> listOrderItemSnapshots(@NotNull Long orderId,
                                                                   @NotNull List<Long> orderItemIds) {
        if (orderItemIds.isEmpty())
            return List.of();
        List<OrderItemPO> rowList = orderItemMapper.selectByIds(orderItemIds);
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .filter(row -> row.getId() != null && row.getOrderId() != null && row.getSkuId() != null
                        && row.getQuantity() != null && row.getUnitPrice() != null)
                .filter(row -> orderId.equals(row.getOrderId()))
                .map(row -> new OrderItemSnapshot(
                        row.getId(),
                        row.getOrderId(),
                        row.getSkuId(),
                        row.getQuantity(),
                        row.getUnitPrice()
                ))
                .toList();
    }

    /**
     * 按物流单 ID 列表查询物流单归属快照
     *
     * @param shipmentIds 物流单 ID 列表
     * @return 物流单归属快照列表
     */
    @Override
    public @NotNull List<ShipmentOrderSnapshot> listShipmentOrderSnapshots(@NotNull List<Long> shipmentIds) {
        if (shipmentIds.isEmpty())
            return List.of();
        List<ShipmentPO> rowList = shipmentMapper.selectByIds(shipmentIds);
        if (rowList == null || rowList.isEmpty())
            return List.of();
        return rowList.stream()
                .filter(row -> row.getId() != null)
                .map(row -> new ShipmentOrderSnapshot(row.getId(), row.getOrderId()))
                .toList();
    }

    /**
     * 按物流单 ID 列表查询已绑定补发单映射
     *
     * @param shipmentIds 物流单 ID 列表
     * @return shipmentId -> reshipId 映射
     */
    @Override
    public @NotNull Map<Long, Long> mapBoundReshipByShipmentIds(@NotNull List<Long> shipmentIds) {
        if (shipmentIds.isEmpty())
            return Map.of();
        List<AfterSalesReshipShipmentPO> rowList = afterSalesReshipShipmentMapper.listByShipmentIds(shipmentIds);
        if (rowList == null || rowList.isEmpty())
            return Map.of();
        Map<Long, Long> bindingMap = new LinkedHashMap<>(rowList.size());
        for (AfterSalesReshipShipmentPO row : rowList) {
            Long shipmentId = row.getShipmentId();
            Long reshipId = row.getReshipId();
            if (shipmentId != null && reshipId != null)
                bindingMap.put(shipmentId, reshipId);
        }
        return bindingMap;
    }

    /**
     * 批量绑定补发单和物流单
     *
     * @param reshipId     补发单 ID
     * @param shipmentIds 物流单 ID 列表
     * @return 影响行数
     */
    @Override
    public int bindReshipShipments(@NotNull Long reshipId,
                                   @NotNull List<Long> shipmentIds) {
        if (shipmentIds.isEmpty())
            return 0;
        List<AfterSalesReshipShipmentPO> insertRows = shipmentIds.stream()
                .map(shipmentId -> AfterSalesReshipShipmentPO.builder()
                        .reshipId(reshipId)
                        .shipmentId(shipmentId)
                        .build())
                .toList();
        return afterSalesReshipShipmentMapper.batchInsert(insertRows);
    }

    /**
     * 持久化行转换为补发单聚合
     *
     * @param row          持久化行
     * @param itemList     补发明细列表
     * @param shipmentList 关联物流单列表
     * @return 补发单聚合
     */
    private @NotNull AfterSalesReship toReshipAggregate(@NotNull AfterSalesReshipPO row,
                                                        @NotNull List<ReshipItem> itemList,
                                                        @NotNull List<ReshipShipment> shipmentList) {
        return AfterSalesReship.reconstitute(
                row.getId(),
                ReshipNo.of(requireColumn(row.getReshipNo(), "reshipNo")),
                requireColumn(row.getOrderId(), "orderId"),
                row.getTicketId(),
                requireColumn(row.getShipmentId(), "shipmentId"),
                ReshipReasonCode.fromValue(requireColumn(row.getReasonCode(), "reasonCode")),
                ReshipStatus.fromValue(requireColumn(row.getStatus(), "status")),
                requireColumn(row.getCurrency(), "currency"),
                row.getItemsCost(),
                row.getShippingCost(),
                row.getNote(),
                requireColumn(row.getCreatedAt(), "createdAt"),
                requireColumn(row.getUpdatedAt(), "updatedAt"),
                itemList,
                shipmentList
        );
    }

    /**
     * 持久化行转换为补发明细实体
     *
     * @param row 持久化行
     * @return 补发明细实体
     */
    private @NotNull ReshipItem toReshipItemEntity(@NotNull AfterSalesReshipItemPO row) {
        return ReshipItem.reconstitute(
                row.getId(),
                requireColumn(row.getReshipId(), "reshipId"),
                requireColumn(row.getOrderItemId(), "orderItemId"),
                requireColumn(row.getSkuId(), "skuId"),
                requireColumn(row.getQuantity(), "quantity"),
                requireColumn(row.getAmount(), "amount"),
                requireColumn(row.getCreatedAt(), "createdAt")
        );
    }

    /**
     * 持久化行转换为补发和物流单关联实体
     *
     * @param row 持久化行
     * @return 补发和物流单关联实体
     */
    private @NotNull ReshipShipment toReshipShipmentEntity(@NotNull AfterSalesReshipShipmentPO row) {
        return ReshipShipment.reconstitute(
                requireColumn(row.getReshipId(), "reshipId"),
                requireColumn(row.getShipmentId(), "shipmentId"),
                requireColumn(row.getCreatedAt(), "createdAt")
        );
    }

    /**
     * 领域聚合转换为补发单持久化行
     *
     * @param reship 补发单聚合
     * @return 补发单持久化行
     */
    private @NotNull AfterSalesReshipPO toInsertReshipPO(@NotNull AfterSalesReship reship) {
        return AfterSalesReshipPO.builder()
                .reshipNo(reship.getReshipNo().getValue())
                .orderId(reship.getOrderId())
                .ticketId(reship.getTicketId())
                .shipmentId(reship.getShipmentId())
                .reasonCode(reship.getReasonCode().name())
                .status(reship.getStatus().name())
                .currency(reship.getCurrency())
                .itemsCost(reship.getItemsCost())
                .shippingCost(reship.getShippingCost())
                .note(reship.getNote())
                .build();
    }

    /**
     * 领域实体转换为补发明细持久化行
     *
     * @param reshipId 补发单 ID
     * @param item     补发明细实体
     * @return 补发明细持久化行
     */
    private @NotNull AfterSalesReshipItemPO toInsertReshipItemPO(@NotNull Long reshipId,
                                                                 @NotNull ReshipItem item) {
        return AfterSalesReshipItemPO.builder()
                .reshipId(reshipId)
                .orderItemId(item.getOrderItemId())
                .skuId(item.getSkuId())
                .quantity(item.getQuantity())
                .build();
    }

    /**
     * 校验列值非空, 返回列值
     *
     * @param value      列值
     * @param columnName 列名
     * @return 非空列值
     */
    private <T> @NotNull T requireColumn(@Nullable T value, @NotNull String columnName) {
        require(value != null, columnName + " 不能为空");
        return value;
    }
}
