package shopping.international.domain.model.aggregate.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.entity.shipping.ShipmentItem;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.domain.model.vo.shipping.*;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 物流单聚合根
 *
 * <p>对应表 {@code shipment}, 聚合内对象包括:</p>
 * <ul>
 *     <li>{@link ShipmentItem}, 对应 {@code shipment_item}</li>
 *     <li>{@link ShipmentStatusLog}, 对应 {@code shipment_status_log}</li>
 * </ul>
 *
 * <p>聚合职责:</p>
 * <ul>
 *     <li>维护物流状态机与状态流转日志的唯一权威逻辑</li>
 *     <li>维护发货前必填约束与面单回填规则</li>
 *     <li>维护轨迹事件幂等去重与防回退规则</li>
 * </ul>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@Accessors(chain = true)
public class Shipment implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 物流单号
     */
    private final ShipmentNo shipmentNo;
    /**
     * 发起来源订单 ID
     */
    @Nullable
    private final Long orderId;
    /**
     * 订单号
     */
    @Nullable
    private final String orderNo;
    /**
     * 幂等键
     */
    @Nullable
    private String idempotencyKey;
    /**
     * 承运商编码
     */
    @Nullable
    private String carrierCode;
    /**
     * 承运商名称
     */
    @Nullable
    private String carrierName;
    /**
     * 服务编码
     */
    @Nullable
    private String serviceCode;
    /**
     * 追踪号
     */
    @Nullable
    private String trackingNo;
    /**
     * 三方物流外部单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 当前物流状态
     */
    private ShipmentStatus status;
    /**
     * 发货地址快照
     */
    @Nullable
    private ShippingAddressSnapshot shipFrom;
    /**
     * 收货地址快照
     */
    @Nullable
    private ShippingAddressSnapshot shipTo;
    /**
     * 包裹尺寸与重量
     */
    @Nullable
    private ShipmentDimension dimension;
    /**
     * 申报价值 (最小货币单位)
     */
    @Nullable
    private Long declaredValue;
    /**
     * 币种
     */
    private String currency;
    /**
     * 关务快照
     */
    @Nullable
    private CustomsInfoSnapshot customsInfo;
    /**
     * 面单 URL
     */
    @Nullable
    private String labelUrl;
    /**
     * 揽收时间
     */
    @Nullable
    private LocalDateTime pickupTime;
    /**
     * 签收时间
     */
    @Nullable
    private LocalDateTime deliveredTime;
    /**
     * 创建时间
     */
    private final LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    /**
     * 物流单下的商品明细列表
     */
    private List<ShipmentItem> itemList;
    /**
     * 物流状态日志列表
     */
    private List<ShipmentStatusLog> statusLogList;

    /**
     * 构造物流单聚合
     *
     * @param id             主键 ID
     * @param shipmentNo     物流单号
     * @param orderId        订单 ID
     * @param orderNo        订单号
     * @param idempotencyKey 幂等键
     * @param carrierCode    承运商编码
     * @param carrierName    承运商名称
     * @param serviceCode    服务编码
     * @param trackingNo     追踪号
     * @param extExternalId  外部单号
     * @param status         当前状态
     * @param shipFrom       发货地址快照
     * @param shipTo         收货地址快照
     * @param dimension      尺寸重量信息
     * @param declaredValue  申报价值
     * @param currency       币种
     * @param customsInfo    关务快照
     * @param labelUrl       面单 URL
     * @param pickupTime     揽收时间
     * @param deliveredTime  签收时间
     * @param itemList       物流单商品明细
     * @param statusLogList  状态日志列表
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     */
    private Shipment(@Nullable Long id,
                     ShipmentNo shipmentNo,
                     @Nullable Long orderId,
                     @Nullable String orderNo,
                     @Nullable String idempotencyKey,
                     @Nullable String carrierCode,
                     @Nullable String carrierName,
                     @Nullable String serviceCode,
                     @Nullable String trackingNo,
                     @Nullable String extExternalId,
                     ShipmentStatus status,
                     @Nullable ShippingAddressSnapshot shipFrom,
                     @Nullable ShippingAddressSnapshot shipTo,
                     @Nullable ShipmentDimension dimension,
                     @Nullable Long declaredValue,
                     @Nullable String currency,
                     @Nullable CustomsInfoSnapshot customsInfo,
                     @Nullable String labelUrl,
                     @Nullable LocalDateTime pickupTime,
                     @Nullable LocalDateTime deliveredTime,
                     @Nullable List<ShipmentItem> itemList,
                     @Nullable List<ShipmentStatusLog> statusLogList,
                     LocalDateTime createdAt,
                     LocalDateTime updatedAt) {
        this.id = id;
        this.shipmentNo = shipmentNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.idempotencyKey = idempotencyKey;
        this.carrierCode = carrierCode;
        this.carrierName = carrierName;
        this.serviceCode = serviceCode;
        this.trackingNo = trackingNo;
        this.extExternalId = extExternalId;
        this.status = status;
        this.shipFrom = shipFrom;
        this.shipTo = shipTo;
        this.dimension = dimension;
        this.declaredValue = declaredValue;
        this.currency = currency;
        this.customsInfo = customsInfo;
        this.labelUrl = labelUrl;
        this.pickupTime = pickupTime;
        this.deliveredTime = deliveredTime;
        this.itemList = itemList == null ? List.of() : List.copyOf(itemList);
        this.statusLogList = statusLogList == null ? List.of() : List.copyOf(statusLogList);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建占位物流单
     *
     * @param shipmentNo     物流单号
     * @param orderId        订单 ID
     * @param orderNo        订单号
     * @param idempotencyKey 幂等键
     * @param shipFrom       发货地址快照
     * @param shipTo         收货地址快照
     * @param declaredValue  申报价值
     * @param currency       币种
     * @param itemList       商品明细列表
     * @param customsInfo    关务快照
     * @return 新建物流单聚合
     */
    public static Shipment createPlaceholder(ShipmentNo shipmentNo,
                                             @Nullable Long orderId,
                                             @Nullable String orderNo,
                                             @Nullable String idempotencyKey,
                                             @Nullable ShippingAddressSnapshot shipFrom,
                                             @Nullable ShippingAddressSnapshot shipTo,
                                             @Nullable Long declaredValue,
                                             @Nullable String currency,
                                             @Nullable List<ShipmentItem> itemList,
                                             @Nullable CustomsInfoSnapshot customsInfo) {
        Shipment shipment = new Shipment(
                null, shipmentNo, orderId, orderNo, idempotencyKey,
                null, null, null, null, null,
                ShipmentStatus.CREATED,
                shipFrom, shipTo, null,
                declaredValue, currency,
                customsInfo, null,
                null, null,
                itemList, List.of(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        shipment.validate();
        return shipment;
    }

    /**
     * 从持久化层重建物流单聚合
     *
     * @param id             主键 ID
     * @param shipmentNo     物流单号
     * @param orderId        订单 ID
     * @param orderNo        订单号
     * @param idempotencyKey 幂等键
     * @param carrierCode    承运商编码
     * @param carrierName    承运商名称
     * @param serviceCode    服务编码
     * @param trackingNo     追踪号
     * @param extExternalId  外部单号
     * @param status         当前状态
     * @param shipFrom       发货地址快照
     * @param shipTo         收货地址快照
     * @param dimension      尺寸重量
     * @param declaredValue  申报价值
     * @param currency       币种
     * @param customsInfo    关务快照
     * @param labelUrl       面单 URL
     * @param pickupTime     揽收时间
     * @param deliveredTime  签收时间
     * @param itemList       商品明细
     * @param statusLogList  状态日志
     * @param createdAt      创建时间
     * @param updatedAt      更新时间
     * @return 重建后的物流单聚合
     */
    public static Shipment reconstitute(Long id,
                                        ShipmentNo shipmentNo,
                                        @Nullable Long orderId,
                                        @Nullable String orderNo,
                                        @Nullable String idempotencyKey,
                                        @Nullable String carrierCode,
                                        @Nullable String carrierName,
                                        @Nullable String serviceCode,
                                        @Nullable String trackingNo,
                                        @Nullable String extExternalId,
                                        ShipmentStatus status,
                                        @Nullable ShippingAddressSnapshot shipFrom,
                                        @Nullable ShippingAddressSnapshot shipTo,
                                        @Nullable ShipmentDimension dimension,
                                        @Nullable Long declaredValue,
                                        String currency,
                                        @Nullable CustomsInfoSnapshot customsInfo,
                                        @Nullable String labelUrl,
                                        @Nullable LocalDateTime pickupTime,
                                        @Nullable LocalDateTime deliveredTime,
                                        @Nullable List<ShipmentItem> itemList,
                                        @Nullable List<ShipmentStatusLog> statusLogList,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        Shipment shipment = new Shipment(id, shipmentNo, orderId, orderNo, idempotencyKey,
                carrierCode, carrierName, serviceCode, trackingNo, extExternalId,
                status, shipFrom, shipTo, dimension, declaredValue, currency,
                customsInfo, labelUrl, pickupTime, deliveredTime,
                itemList, statusLogList, createdAt, updatedAt);
        shipment.validate();
        return shipment;
    }

    /**
     * 设置聚合主键 ID
     *
     * @param id 主键 ID
     */
    public void assignId(Long id) {
        requireNotNull(id, "id 不能为空");
        require(id > 0, "id 必须大于 0");
        if (this.id != null)
            require(Objects.equals(this.id, id), "id 不允许被修改");
        this.id = id;

        List<ShipmentItem> normalizedItems = new ArrayList<>(itemList.size());
        for (ShipmentItem item : itemList) {
            item.bindShipmentId(id);
            normalizedItems.add(item);
        }
        this.itemList = List.copyOf(normalizedItems);
        touch();
    }

    /**
     * 回填面单信息
     *
     * <p>该操作只更新面单字段, 不推进物流状态</p>
     *
     * @param label 面单信息
     */
    public void fillLabel(@NotNull ShipmentLabel label) {
        requireNotNull(label, "label 不能为空");
        label.validate();
        requireStatus(!status.isStrongFinalState(), "强终态不允许回填面单信息");

        carrierCode = label.getCarrierCode();
        carrierName = label.getCarrierName();
        trackingNo = label.getTrackingNo();

        if (label.getServiceCode() != null)
            serviceCode = label.getServiceCode();
        if (label.getExtExternalId() != null)
            extExternalId = label.getExtExternalId();
        if (label.getLabelUrl() != null)
            labelUrl = label.getLabelUrl();
        if (label.getDimension() != null)
            dimension = label.getDimension();
        if (label.getDeclaredValue() != null)
            declaredValue = label.getDeclaredValue();
        if (label.getCurrency() != null)
            currency = label.getCurrency();

        touch();
    }

    /**
     * 绑定发货与收货地址快照
     *
     * @param shipFrom 发货地址
     * @param shipTo   收货地址
     */
    public void bindAddressSnapshots(@Nullable ShippingAddressSnapshot shipFrom,
                                     @Nullable ShippingAddressSnapshot shipTo) {
        requireNotNull(shipFrom, "shipFrom 不能为空");
        requireNotNull(shipTo, "shipTo 不能为空");
        shipFrom.validate();
        shipTo.validate();
        this.shipFrom = shipFrom;
        this.shipTo = shipTo;
        touch();
    }

    /**
     * 合并关务信息快照
     *
     * @param customsInfo 关务快照
     */
    public void mergeCustomsInfo(@Nullable CustomsInfoSnapshot customsInfo) {
        if (customsInfo == null)
            return;
        customsInfo.validate();
        this.customsInfo = this.customsInfo == null ? customsInfo : this.customsInfo.merge(customsInfo.getExtra());
        touch();
    }

    /**
     * 添加物流单商品明细
     *
     * @param item 物流单商品实体
     */
    public void addItem(@NotNull ShipmentItem item) {
        requireNotNull(item, "item 不能为空");
        item.validate();
        if (id != null)
            item.bindShipmentId(id);
        if (orderId != null)
            require(Objects.equals(orderId, item.getOrderId()), "shipment.orderId 与 shipmentItem.orderId 不一致");

        boolean duplicated = itemList.stream()
                .anyMatch(existing -> Objects.equals(existing.getOrderItemId(), item.getOrderItemId()));
        require(!duplicated, "同一物流单不允许重复添加同一 orderItemId");

        List<ShipmentItem> merged = new ArrayList<>(itemList);
        merged.add(item);
        itemList = List.copyOf(merged);
        touch();
    }

    /**
     * 执行批量发货动作
     *
     * <p>该方法会先执行发货前必填校验, 再统一推进到 {@link ShipmentStatus#LABEL_CREATED}</p>
     *
     * @param sourceType  事件来源
     * @param sourceRef   来源引用
     * @param note        备注
     * @param eventTime   事件时间
     * @param rawPayload  原始报文
     * @param actorUserId 操作者用户 ID
     * @return 新增或复用的状态日志
     */
    public ShipmentStatusLog dispatch(@NotNull ShipmentStatusEventSource sourceType,
                                      @NotNull String sourceRef,
                                      @Nullable String note,
                                      @Nullable LocalDateTime eventTime,
                                      @Nullable java.util.Map<String, Object> rawPayload,
                                      @Nullable Long actorUserId) {
        ensureDispatchReady();
        ShipmentTrackingEvent event = ShipmentTrackingEvent.transition(
                ShipmentStatus.LABEL_CREATED,
                eventTime,
                sourceType,
                sourceRef,
                carrierCode,
                trackingNo,
                note,
                rawPayload,
                actorUserId
        );
        return applyTrackingEvent(event);
    }

    /**
     * 应用轨迹事件并推进状态
     *
     * <p>规则说明:</p>
     * <ul>
     *     <li>同 {@code sourceType + sourceRef} 事件幂等去重</li>
     *     <li>强终态禁止继续流转</li>
     *     <li>主链路状态具备防回退保护</li>
     *     <li>旁路状态 {@link ShipmentStatus#EXCEPTION} 允许恢复到主链路</li>
     * </ul>
     *
     * @param event 轨迹事件
     * @return 新增或复用的状态日志
     */
    public ShipmentStatusLog applyTrackingEvent(@NotNull ShipmentTrackingEvent event) {
        requireNotNull(event, "event 不能为空");
        event.validate();
        Long persistedId = requirePersistedId();

        ShipmentStatusLog existingLog = findByDedupeKey(event.getSourceType(), event.getSourceRef());
        if (existingLog != null) {
            if (event.getToStatus() != null)
                requireStatus(existingLog.getToStatus() == event.getToStatus(),
                        "同一 sourceType + sourceRef 重复请求的目标状态不一致");
            return existingLog;
        }

        ShipmentStatus fromStatus = status;
        LocalDateTime effectiveEventTime = event.getEventTime() == null ? LocalDateTime.now() : event.getEventTime();

        ShipmentStatus targetStatus = event.getToStatus();
        if (targetStatus == null)
            return appendStatusLog(
                    ShipmentStatusLog.create(
                            persistedId, fromStatus, fromStatus,
                            effectiveEventTime, event.getSourceType(), event.getSourceRef(),
                            event.getCarrierCode(), event.getTrackingNo(), event.getNote(),
                            event.getRawPayload(), event.getRawPayloadText(), event.getActorUserId()
                    )
            );

        if (fromStatus == targetStatus)
            return appendStatusLog(
                    ShipmentStatusLog.create(
                            persistedId, fromStatus, targetStatus,
                            effectiveEventTime, event.getSourceType(), event.getSourceRef(),
                            event.getCarrierCode(), event.getTrackingNo(), event.getNote(),
                            event.getRawPayload(), event.getRawPayloadText(), event.getActorUserId()
                    )
            );

        requireStatus(!fromStatus.isStrongFinalState(), "强终态不允许继续流转");
        requireStatus(canTransit(fromStatus, targetStatus),
                "状态不允许从 " + fromStatus + " 流转到 " + targetStatus);
        requireStatus(!targetStatus.isRollbackComparedTo(fromStatus),
                "检测到状态回退, 当前状态: " + fromStatus + ", 目标状态: " + targetStatus);

        status = targetStatus;

        if (event.getCarrierCode() != null && carrierCode == null)
            carrierCode = event.getCarrierCode();
        if (event.getTrackingNo() != null && trackingNo == null)
            trackingNo = event.getTrackingNo();

        if (targetStatus == ShipmentStatus.PICKED_UP && pickupTime == null)
            pickupTime = effectiveEventTime;
        if (targetStatus == ShipmentStatus.DELIVERED)
            deliveredTime = effectiveEventTime;

        return appendStatusLog(
                ShipmentStatusLog.create(
                        persistedId, fromStatus, targetStatus,
                        effectiveEventTime, event.getSourceType(), event.getSourceRef(),
                        event.getCarrierCode(), event.getTrackingNo(), event.getNote(),
                        event.getRawPayload(), event.getRawPayloadText(), event.getActorUserId()
                )
        );
    }

    /**
     * 判断当前物流单是否允许用户改址
     *
     * @return 仅当状态为 {@code CREATED/LABEL_CREATED} 时返回 {@code true}
     */
    public boolean isAddressChangeAllowed() {
        return status == ShipmentStatus.CREATED || status == ShipmentStatus.LABEL_CREATED;
    }

    /**
     * 追加状态日志
     *
     * @param log 状态日志
     * @return 被追加的日志
     */
    private ShipmentStatusLog appendStatusLog(@NotNull ShipmentStatusLog log) {
        requireNotNull(log, "log 不能为空");
        log.validate();

        ShipmentStatusLog duplicated = findByDedupeKey(log.getSourceType(), log.getSourceRef());
        if (duplicated != null) {
            requireStatus(duplicated.getToStatus() == log.getToStatus(),
                    "同一 sourceType + sourceRef 重复请求的目标状态不一致");
            return duplicated;
        }

        List<ShipmentStatusLog> merged = new ArrayList<>(statusLogList);
        merged.add(log);
        statusLogList = List.copyOf(merged);
        touch();
        return log;
    }

    /**
     * 根据去重键查找属于本物流单的已存在日志
     *
     * @param sourceType 来源类型
     * @param sourceRef  来源引用
     * @return 已存在日志, 不存在时返回 {@code null}
     */
    private @Nullable ShipmentStatusLog findByDedupeKey(@NotNull ShipmentStatusEventSource sourceType,
                                                        @NotNull String sourceRef) {
        return statusLogList.stream()
                .filter(log -> log.getSourceType() == sourceType && Objects.equals(log.getSourceRef(), sourceRef))
                .findFirst()
                .orElse(null);
    }

    /**
     * 校验状态流转矩阵
     *
     * @param fromStatus 当前状态
     * @param toStatus   目标状态
     * @return 若允许流转返回 {@code true}, 否则返回 {@code false}
     */
    private static boolean canTransit(@NotNull ShipmentStatus fromStatus,
                                      @NotNull ShipmentStatus toStatus) {
        requireNotNull(fromStatus, "fromStatus 不能为空");
        requireNotNull(toStatus, "toStatus 不能为空");

        return switch (toStatus) {
            case CREATED, LABEL_CREATED -> fromStatus == ShipmentStatus.CREATED;
            case PICKED_UP -> fromStatus == ShipmentStatus.LABEL_CREATED;
            case IN_TRANSIT -> fromStatus == ShipmentStatus.LABEL_CREATED
                    || fromStatus == ShipmentStatus.PICKED_UP
                    || fromStatus == ShipmentStatus.EXCEPTION;
            case CUSTOMS_PROCESSING -> fromStatus == ShipmentStatus.IN_TRANSIT
                    || fromStatus == ShipmentStatus.HANDED_OVER;
            case CUSTOMS_HOLD -> fromStatus == ShipmentStatus.CUSTOMS_PROCESSING
                    || fromStatus == ShipmentStatus.IN_TRANSIT;
            case CUSTOMS_RELEASED -> fromStatus == ShipmentStatus.CUSTOMS_PROCESSING
                    || fromStatus == ShipmentStatus.CUSTOMS_HOLD
                    || fromStatus == ShipmentStatus.IN_TRANSIT;
            case HANDED_OVER -> fromStatus == ShipmentStatus.IN_TRANSIT
                    || fromStatus == ShipmentStatus.CUSTOMS_RELEASED;
            case OUT_FOR_DELIVERY -> fromStatus == ShipmentStatus.HANDED_OVER
                    || fromStatus == ShipmentStatus.IN_TRANSIT
                    || fromStatus == ShipmentStatus.CUSTOMS_RELEASED
                    || fromStatus == ShipmentStatus.EXCEPTION;
            case DELIVERED, EXCEPTION, RETURNED, LOST, CANCELLED -> !fromStatus.isStrongFinalState();
        };
    }

    /**
     * 获取已持久化的物流单 ID
     *
     * @return 已持久化 ID
     */
    private Long requirePersistedId() {
        requireNotNull(id, "shipment.id 不能为空, 请先持久化后再记录状态日志");
        return id;
    }

    /**
     * 执行批量发货前的必填校验
     */
    private void ensureDispatchReady() {
        requireNotNull(id, "dispatch 前 id 不能为空");
        requireNotNull(shipmentNo, "dispatch 前 shipmentNo 不能为空");
        requireNotNull(orderId, "dispatch 前 orderId 不能为空");
        requireNotNull(idempotencyKey, "dispatch 前 idempotencyKey 不能为空");
        requireNotNull(carrierCode, "dispatch 前 carrierCode 不能为空");
        requireNotNull(carrierName, "dispatch 前 carrierName 不能为空");
        requireNotNull(trackingNo, "dispatch 前 trackingNo 不能为空");
        requireNotNull(status, "dispatch 前 status 不能为空");
        requireNotNull(shipFrom, "dispatch 前 shipFrom 不能为空");
        requireNotNull(shipTo, "dispatch 前 shipTo 不能为空");
        requireNotNull(declaredValue, "dispatch 前 declaredValue 不能为空");
        requireNotNull(currency, "dispatch 前 currency 不能为空");
    }

    /**
     * 更新时间戳
     */
    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 断言状态机条件
     *
     * @param ok      条件表达式
     * @param message 失败提示
     */
    private static void requireStatus(boolean ok, String message) {
        if (!ok)
            throw new ConflictException(message);
    }

    /**
     * 校验物流聚合不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        if (orderId != null)
            require(orderId > 0, "orderId 必须大于 0");

        requireNotNull(shipmentNo, "shipmentNo 不能为空");
        shipmentNo.validate();

        idempotencyKey = normalizeNullableField(idempotencyKey, "idempotencyKey 不能为空",
                value -> value.length() <= 64,
                "idempotencyKey 长度不能超过 64 个字符");
        carrierCode = normalizeNullableField(carrierCode, "carrierCode 不能为空",
                value -> value.length() <= 64,
                "carrierCode 长度不能超过 64 个字符");
        carrierName = normalizeNullableField(carrierName, "carrierName 不能为空",
                value -> value.length() <= 128,
                "carrierName 长度不能超过 128 个字符");
        serviceCode = normalizeNullableField(serviceCode, "serviceCode 不能为空",
                value -> value.length() <= 64,
                "serviceCode 长度不能超过 64 个字符");
        trackingNo = normalizeNullableField(trackingNo, "trackingNo 不能为空",
                value -> value.length() <= 128,
                "trackingNo 长度不能超过 128 个字符");
        extExternalId = normalizeNullableField(extExternalId, "extExternalId 不能为空",
                value -> value.length() <= 128,
                "extExternalId 长度不能超过 128 个字符");

        requireNotNull(status, "status 不能为空");

        if (shipFrom != null)
            shipFrom.validate();
        if (shipTo != null)
            shipTo.validate();
        if (dimension != null)
            dimension.validate();

        if (declaredValue != null)
            require(declaredValue >= 0L, "declaredValue 不能为负数");
        currency = normalizeCurrency(currency);

        if (customsInfo != null)
            customsInfo.validate();

        labelUrl = normalizeNullableField(labelUrl, "labelUrl 不能为空",
                value -> value.length() <= 500,
                "labelUrl 长度不能超过 500 个字符");

        itemList = normalizeDistinctList(itemList,
                ShipmentItem::validate,
                ShipmentItem::getOrderItemId,
                "同一物流单中 orderItemId 不能重复");

        if (id != null) {
            for (ShipmentItem item : itemList)
                item.bindShipmentId(id);
        }
        if (orderId != null) {
            for (ShipmentItem item : itemList)
                require(Objects.equals(orderId, item.getOrderId()), "shipment.orderId 与 shipmentItem.orderId 不一致");
        }

        statusLogList = normalizeDistinctList(statusLogList,
                ShipmentStatusLog::validate,
                ShipmentStatusLog::dedupeKey,
                "sourceType + sourceRef 不能重复");

        if (status == ShipmentStatus.DELIVERED)
            requireNotNull(deliveredTime, "DELIVERED 状态下 deliveredTime 不能为空");

        requireNotNull(createdAt, "createdAt 不能为空");
        requireNotNull(updatedAt, "updatedAt 不能为空");
    }
}
