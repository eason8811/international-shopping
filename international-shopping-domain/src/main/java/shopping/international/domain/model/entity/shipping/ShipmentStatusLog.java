package shopping.international.domain.model.entity.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 物流状态流转日志实体
 *
 * <p>对应表 {@code shipment_status_log}, 建议采用追加写</p>
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@Accessors(chain = true)
public class ShipmentStatusLog implements Verifiable {

    /**
     * 主键 ID, 未持久化时可为空
     */
    @Nullable
    private Long id;
    /**
     * 物流单 ID
     */
    private Long shipmentId;
    /**
     * 变更前状态
     */
    @Nullable
    private ShipmentStatus fromStatus;
    /**
     * 变更后状态
     */
    private ShipmentStatus toStatus;
    /**
     * 事件发生时间
     */
    @Nullable
    private LocalDateTime eventTime;
    /**
     * 事件来源类型
     */
    private ShipmentStatusEventSource sourceType;
    /**
     * 来源引用 ID
     */
    private String sourceRef;
    /**
     * 承运商编码
     */
    @Nullable
    private String carrierCode;
    /**
     * 追踪号
     */
    @Nullable
    private String trackingNo;
    /**
     * 备注
     */
    @Nullable
    private String note;
    /**
     * 原始报文
     */
    @Nullable
    private Map<String, Object> rawPayload;
    /**
     * 原始报文文本, 用于保真落库 (如 WebHook 原始 JSON)
     */
    @Nullable
    private String rawPayloadText;
    /**
     * 操作者用户 ID
     */
    @Nullable
    private Long actorUserId;
    /**
     * 写入时间
     */
    private LocalDateTime createdAt;

    /**
     * 构造物流状态日志实体
     *
     * @param id 主键 ID
     * @param shipmentId 物流单 ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param eventTime 事件发生时间
     * @param sourceType 事件来源类型
     * @param sourceRef 来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo 追踪号
     * @param note 备注
     * @param rawPayload 原始报文
     * @param rawPayloadText 原始报文文本
     * @param actorUserId 操作者用户 ID
     * @param createdAt 写入时间
     */
    private ShipmentStatusLog(@Nullable Long id,
                              Long shipmentId,
                              @Nullable ShipmentStatus fromStatus,
                              ShipmentStatus toStatus,
                              @Nullable LocalDateTime eventTime,
                              ShipmentStatusEventSource sourceType,
                              String sourceRef,
                              @Nullable String carrierCode,
                              @Nullable String trackingNo,
                              @Nullable String note,
                              @Nullable Map<String, Object> rawPayload,
                              @Nullable String rawPayloadText,
                              @Nullable Long actorUserId,
                              LocalDateTime createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.eventTime = eventTime;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.note = note;
        this.rawPayload = rawPayload;
        this.rawPayloadText = rawPayloadText;
        this.actorUserId = actorUserId;
        this.createdAt = createdAt;
    }

    /**
     * 创建新的物流状态日志
     *
     * @param shipmentId 物流单 ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param eventTime 事件发生时间
     * @param sourceType 事件来源类型
     * @param sourceRef 来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo 追踪号
     * @param note 备注
     * @param rawPayload 原始报文
     * @param actorUserId 操作者用户 ID
     * @return 新建日志实体
     */
    public static ShipmentStatusLog create(Long shipmentId,
                                           @Nullable ShipmentStatus fromStatus,
                                           ShipmentStatus toStatus,
                                           @Nullable LocalDateTime eventTime,
                                           ShipmentStatusEventSource sourceType,
                                           String sourceRef,
                                           @Nullable String carrierCode,
                                           @Nullable String trackingNo,
                                           @Nullable String note,
                                           @Nullable Map<String, Object> rawPayload,
                                           @Nullable Long actorUserId) {
        return create(
                shipmentId,
                fromStatus,
                toStatus,
                eventTime,
                sourceType,
                sourceRef,
                carrierCode,
                trackingNo,
                note,
                rawPayload,
                null,
                actorUserId
        );
    }

    /**
     * 创建新的物流状态日志
     *
     * @param shipmentId 物流单 ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param eventTime 事件发生时间
     * @param sourceType 事件来源类型
     * @param sourceRef 来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo 追踪号
     * @param note 备注
     * @param rawPayload 原始报文
     * @param rawPayloadText 原始报文文本
     * @param actorUserId 操作者用户 ID
     * @return 新建日志实体
     */
    public static ShipmentStatusLog create(Long shipmentId,
                                           @Nullable ShipmentStatus fromStatus,
                                           ShipmentStatus toStatus,
                                           @Nullable LocalDateTime eventTime,
                                           ShipmentStatusEventSource sourceType,
                                           String sourceRef,
                                           @Nullable String carrierCode,
                                           @Nullable String trackingNo,
                                           @Nullable String note,
                                           @Nullable Map<String, Object> rawPayload,
                                           @Nullable String rawPayloadText,
                                           @Nullable Long actorUserId) {
        ShipmentStatusLog log = new ShipmentStatusLog(null, shipmentId, fromStatus, toStatus, eventTime,
                sourceType, sourceRef, carrierCode, trackingNo, note,
                rawPayload == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(rawPayload)),
                rawPayloadText,
                actorUserId, LocalDateTime.now());
        log.validate();
        return log;
    }

    /**
     * 从持久化层重建物流状态日志
     *
     * @param id 主键 ID
     * @param shipmentId 物流单 ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param eventTime 事件发生时间
     * @param sourceType 事件来源类型
     * @param sourceRef 来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo 追踪号
     * @param note 备注
     * @param rawPayload 原始报文
     * @param actorUserId 操作者用户 ID
     * @param createdAt 写入时间
     * @return 重建后的日志实体
     */
    public static ShipmentStatusLog reconstitute(Long id,
                                                 Long shipmentId,
                                                 @Nullable ShipmentStatus fromStatus,
                                                 ShipmentStatus toStatus,
                                                 @Nullable LocalDateTime eventTime,
                                                 ShipmentStatusEventSource sourceType,
                                                 String sourceRef,
                                                 @Nullable String carrierCode,
                                                 @Nullable String trackingNo,
                                                 @Nullable String note,
                                                 @Nullable Map<String, Object> rawPayload,
                                                 @Nullable Long actorUserId,
                                                 LocalDateTime createdAt) {
        return reconstitute(
                id,
                shipmentId,
                fromStatus,
                toStatus,
                eventTime,
                sourceType,
                sourceRef,
                carrierCode,
                trackingNo,
                note,
                rawPayload,
                null,
                actorUserId,
                createdAt
        );
    }

    /**
     * 从持久化层重建物流状态日志
     *
     * @param id 主键 ID
     * @param shipmentId 物流单 ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param eventTime 事件发生时间
     * @param sourceType 事件来源类型
     * @param sourceRef 来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo 追踪号
     * @param note 备注
     * @param rawPayload 原始报文
     * @param rawPayloadText 原始报文文本
     * @param actorUserId 操作者用户 ID
     * @param createdAt 写入时间
     * @return 重建后的日志实体
     */
    public static ShipmentStatusLog reconstitute(Long id,
                                                 Long shipmentId,
                                                 @Nullable ShipmentStatus fromStatus,
                                                 ShipmentStatus toStatus,
                                                 @Nullable LocalDateTime eventTime,
                                                 ShipmentStatusEventSource sourceType,
                                                 String sourceRef,
                                                 @Nullable String carrierCode,
                                                 @Nullable String trackingNo,
                                                 @Nullable String note,
                                                 @Nullable Map<String, Object> rawPayload,
                                                 @Nullable String rawPayloadText,
                                                 @Nullable Long actorUserId,
                                                 LocalDateTime createdAt) {
        ShipmentStatusLog log = new ShipmentStatusLog(id, shipmentId, fromStatus, toStatus, eventTime,
                sourceType, sourceRef, carrierCode, trackingNo, note,
                rawPayload == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(rawPayload)),
                rawPayloadText,
                actorUserId, createdAt);
        log.validate();
        return log;
    }

    /**
     * 获取幂等去重键
     *
     * @return 去重键, 由 {@code sourceType:sourceRef} 构成
     */
    public String dedupeKey() {
        return sourceType.name() + ":" + sourceRef;
    }

    /**
     * 校验日志实体不变式
     */
    @Override
    public void validate() {
        if (id != null)
            require(id > 0, "id 必须大于 0");
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId > 0, "shipmentId 必须大于 0");
        requireNotNull(toStatus, "toStatus 不能为空");
        requireNotNull(sourceType, "sourceType 不能为空");
        sourceRef = normalizeNotNullField(sourceRef, "sourceRef 不能为空",
                value -> value.length() <= 128,
                "sourceRef 长度不能超过 128 个字符");
        carrierCode = normalizeNullableField(carrierCode, "carrierCode 不能为空",
                value -> value.length() <= 64,
                "carrierCode 长度不能超过 64 个字符");
        trackingNo = normalizeNullableField(trackingNo, "trackingNo 不能为空",
                value -> value.length() <= 128,
                "trackingNo 长度不能超过 128 个字符");
        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");
        if (rawPayloadText != null)
            require(!rawPayloadText.isBlank(), "rawPayloadText 不能为空白");
        if (actorUserId != null)
            require(actorUserId > 0, "actorUserId 必须大于 0");
        requireNotNull(createdAt, "createdAt 不能为空");
    }
}
