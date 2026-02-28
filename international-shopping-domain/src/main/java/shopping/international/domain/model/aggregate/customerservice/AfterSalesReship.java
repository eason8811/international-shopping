package shopping.international.domain.model.aggregate.customerservice;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.customerservice.ReshipItem;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;
import shopping.international.domain.model.enums.customerservice.ReshipReasonCode;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.domain.model.vo.customerservice.ReshipNo;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.normalizeCurrency;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 售后补发单聚合根, 对应表 `aftersales_reship`
 *
 * <p>聚合职责, 维护补发状态机, 明细去重约束, 包裹绑定约束</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "reshipNo")
@Accessors(chain = true)
public class AfterSalesReship implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private final Long id;
    /**
     * 补发单号
     */
    private final ReshipNo reshipNo;
    /**
     * 原订单 ID
     */
    private final Long orderId;
    /**
     * 关联工单 ID
     */
    @Nullable
    private final Long ticketId;
    /**
     * 原物流单 ID
     */
    private final Long shipmentId;
    /**
     * 补发原因
     */
    private final ReshipReasonCode reasonCode;
    /**
     * 补发状态
     */
    private ReshipStatus status;
    /**
     * 币种
     */
    private String currency;
    /**
     * 货品成本 Minor 形式
     */
    @Nullable
    private Long itemsCost;
    /**
     * 运费成本 Minor 形式
     */
    @Nullable
    private Long shippingCost;
    /**
     * 备注
     */
    @Nullable
    private String note;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 补发明细列表
     */
    private List<ReshipItem> itemList;
    /**
     * 关联物流单列表
     */
    private List<ReshipShipment> shipmentList;

    /**
     * 构造售后补发单聚合根
     *
     * @param id           主键 ID
     * @param reshipNo     补发单号
     * @param orderId      原订单 ID
     * @param ticketId     关联工单 ID
     * @param shipmentId   原物流单 ID
     * @param reasonCode   补发原因
     * @param status       补发状态
     * @param currency     币种
     * @param itemsCost    货品成本
     * @param shippingCost 运费成本
     * @param note         备注
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @param itemList     补发明细列表
     * @param shipmentList 关联物流单列表
     */
    private AfterSalesReship(@Nullable Long id,
                             ReshipNo reshipNo,
                             Long orderId,
                             @Nullable Long ticketId,
                             Long shipmentId,
                             ReshipReasonCode reasonCode,
                             ReshipStatus status,
                             String currency,
                             @Nullable Long itemsCost,
                             @Nullable Long shippingCost,
                             @Nullable String note,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             List<ReshipItem> itemList,
                             List<ReshipShipment> shipmentList) {
        this.id = id;
        this.reshipNo = reshipNo;
        this.orderId = orderId;
        this.ticketId = ticketId;
        this.shipmentId = shipmentId;
        this.reasonCode = reasonCode;
        this.status = status;
        this.currency = currency;
        this.itemsCost = itemsCost;
        this.shippingCost = shippingCost;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.itemList = itemList;
        this.shipmentList = shipmentList;
    }

    /**
     * 创建新的售后补发单聚合根
     *
     * @param orderId    原订单 ID
     * @param ticketId   关联工单 ID
     * @param shipmentId 原物流单 ID
     * @param reasonCode 补发原因
     * @param currency   币种
     * @param note       备注
     * @param itemList   补发明细列表
     * @return 新建的售后补发单聚合根
     */
    public static AfterSalesReship create(Long orderId,
                                          @Nullable Long ticketId,
                                          Long shipmentId,
                                          ReshipReasonCode reasonCode,
                                          @Nullable String currency,
                                          @Nullable String note,
                                          @Nullable List<ReshipItem> itemList) {
        LocalDateTime now = LocalDateTime.now();
        AfterSalesReship reship = new AfterSalesReship(
                null,
                ReshipNo.generate(),
                orderId,
                ticketId,
                shipmentId,
                reasonCode,
                ReshipStatus.INIT,
                normalizeCurrency(currency),
                null,
                null,
                normalizeNullableField(note, "note 不能为空", value -> value.length() <= 255, "note 长度不能超过 255"),
                now,
                now,
                normalizeItemList(itemList),
                new ArrayList<>()
        );
        reship.validate();
        return reship;
    }

    /**
     * 从持久化数据重建售后补发单聚合根
     *
     * @param id           主键 ID
     * @param reshipNo     补发单号
     * @param orderId      原订单 ID
     * @param ticketId     关联工单 ID
     * @param shipmentId   原物流单 ID
     * @param reasonCode   补发原因
     * @param status       补发状态
     * @param currency     币种
     * @param itemsCost    货品成本
     * @param shippingCost 运费成本
     * @param note         备注
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @param itemList     补发明细列表
     * @param shipmentList 关联物流单列表
     * @return 重建后的售后补发单聚合根
     */
    public static AfterSalesReship reconstitute(@Nullable Long id,
                                                ReshipNo reshipNo,
                                                Long orderId,
                                                @Nullable Long ticketId,
                                                Long shipmentId,
                                                ReshipReasonCode reasonCode,
                                                ReshipStatus status,
                                                String currency,
                                                @Nullable Long itemsCost,
                                                @Nullable Long shippingCost,
                                                @Nullable String note,
                                                LocalDateTime createdAt,
                                                LocalDateTime updatedAt,
                                                @Nullable List<ReshipItem> itemList,
                                                @Nullable List<ReshipShipment> shipmentList) {
        AfterSalesReship reship = new AfterSalesReship(
                id,
                reshipNo,
                orderId,
                ticketId,
                shipmentId,
                reasonCode,
                status,
                normalizeCurrency(currency),
                itemsCost,
                shippingCost,
                normalizeNullableField(note, "note 不能为空", value -> value.length() <= 255, "note 长度不能超过 255"),
                createdAt,
                updatedAt,
                normalizeItemList(itemList),
                normalizeShipmentList(shipmentList)
        );
        reship.validate();
        return reship;
    }

    /**
     * 推进补发状态
     *
     * @param toStatus 目标状态
     */
    public void transitionStatus(ReshipStatus toStatus) {
        requireNotNull(toStatus, "toStatus 不能为空");
        if (!status.canTransitTo(toStatus))
            throw new ConflictException("当前补发状态不允许流转到目标状态");
        this.status = toStatus;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 更新补发单元数据
     *
     * @param currency     币种
     * @param itemsCost    货品成本
     * @param shippingCost 运费成本
     * @param note         备注
     */
    public void patch(@Nullable String currency,
                      @Nullable Long itemsCost,
                      @Nullable Long shippingCost,
                      @Nullable String note) {
        if (currency != null)
            this.currency = normalizeCurrency(currency);
        if (itemsCost != null) {
            require(itemsCost >= 0, "itemsCost 必须大于等于 0");
            this.itemsCost = itemsCost;
        }
        if (shippingCost != null) {
            require(shippingCost >= 0, "shippingCost 必须大于等于 0");
            this.shippingCost = shippingCost;
        }
        if (note != null)
            this.note = normalizeNullableField(note, "note 不能为空", value -> value.length() <= 255, "note 长度不能超过 255");
        if (currency != null || itemsCost != null || shippingCost != null || note != null)
            this.updatedAt = LocalDateTime.now();
        validate();
    }

    /**
     * 新增补发明细
     *
     * @param orderItemId 原订单明细 ID
     * @param skuId       SKU ID
     * @param quantity    补发数量
     * @return 新增的补发明细实体
     */
    public ReshipItem addItem(Long orderItemId, Long skuId, Integer quantity, Long amount) {
        Long currentReshipId = requirePersisted();
        boolean exists = itemList.stream().anyMatch(item -> Objects.equals(item.getOrderItemId(), orderItemId));
        if (exists)
            throw new ConflictException("同一补发单内不允许重复添加相同 orderItemId");
        ReshipItem item = ReshipItem.create(currentReshipId, orderItemId, skuId, quantity, amount);
        itemList.add(item);
        itemsCost = itemList.stream().map(ReshipItem::getAmount).mapToLong(Long::longValue).sum();
        this.updatedAt = LocalDateTime.now();
        validate();
        return item;
    }

    /**
     * 绑定关联物流单
     *
     * @param shipmentId 物流单 ID
     * @return 新增的补发单和物流单关联实体
     */
    public ReshipShipment bindShipment(Long shipmentId) {
        Long currentReshipId = requirePersisted();
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId > 0, "shipmentId 必须大于 0");
        boolean exists = shipmentList.stream().anyMatch(item -> Objects.equals(item.getShipmentId(), shipmentId));
        if (exists)
            throw new ConflictException("当前补发单已绑定该物流单");
        ReshipShipment shipment = ReshipShipment.create(currentReshipId, shipmentId);
        shipmentList.add(shipment);
        this.updatedAt = LocalDateTime.now();
        validate();
        return shipment;
    }

    /**
     * 批量绑定关联物流单
     *
     * @param shipmentIds 物流单 ID 列表
     */
    public void bindShipments(List<Long> shipmentIds) {
        requireNotNull(shipmentIds, "shipmentIds 不能为空");
        require(!shipmentIds.isEmpty(), "shipmentIds 不能为空数组");
        for (Long shipmentId : shipmentIds)
            bindShipment(shipmentId);
    }

    /**
     * 校验售后补发单聚合不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        requireNotNull(reshipNo, "reshipNo 不能为空");
        requireNotNull(orderId, "orderId 不能为空");
        require(orderId > 0, "orderId 必须大于 0");
        if (ticketId != null)
            require(ticketId > 0, "ticketId 必须大于 0");
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId > 0, "shipmentId 必须大于 0");
        requireNotNull(reasonCode, "reasonCode 不能为空");
        requireNotNull(status, "status 不能为空");
        currency = normalizeCurrency(currency);
        if (itemsCost != null)
            require(itemsCost >= 0, "itemsCost 必须大于等于 0");
        if (shippingCost != null)
            require(shippingCost >= 0, "shippingCost 必须大于等于 0");
        if (note != null)
            note = normalizeNullableField(note, "note 不能为空", value -> value.length() <= 255, "note 长度不能超过 255");
        requireNotNull(createdAt, "createdAt 不能为空");
        requireNotNull(updatedAt, "updatedAt 不能为空");

        itemList = normalizeItemList(itemList);
        shipmentList = normalizeShipmentList(shipmentList);

        Set<Long> orderItemDedup = new LinkedHashSet<>();
        for (ReshipItem item : itemList) {
            if (id != null)
                require(Objects.equals(item.getReshipId(), id), "补发明细 reshipId 与补发单 id 不一致");
            require(orderItemDedup.add(item.getOrderItemId()), "同一补发单内 orderItemId 不允许重复");
        }

        Set<Long> shipmentDedup = new LinkedHashSet<>();
        for (ReshipShipment shipment : shipmentList) {
            if (id != null)
                require(Objects.equals(shipment.getReshipId(), id), "关联物流单 reshipId 与补发单 id 不一致");
            require(shipmentDedup.add(shipment.getShipmentId()), "同一补发单内 shipmentId 不允许重复");
        }
    }

    /**
     * 确保补发单已持久化
     *
     * @return 持久化后的补发单 ID
     */
    private Long requirePersisted() {
        requireNotNull(id, "补发单未持久化, 当前操作不可执行");
        require(id > 0, "补发单 ID 非法");
        return id;
    }

    /**
     * 规范化补发明细列表
     *
     * @param values 原始补发明细列表
     * @return 规范化后的补发明细列表
     */
    private static List<ReshipItem> normalizeItemList(@Nullable List<ReshipItem> values) {
        if (values == null || values.isEmpty())
            return new ArrayList<>();
        return new ArrayList<>(values.stream().peek(ReshipItem::validate).toList());
    }

    /**
     * 规范化补发单和物流单关联列表
     *
     * @param values 原始关联列表
     * @return 规范化后的关联列表
     */
    private static List<ReshipShipment> normalizeShipmentList(@Nullable List<ReshipShipment> values) {
        if (values == null || values.isEmpty())
            return new ArrayList<>();
        return new ArrayList<>(values.stream().peek(ReshipShipment::validate).toList());
    }
}
