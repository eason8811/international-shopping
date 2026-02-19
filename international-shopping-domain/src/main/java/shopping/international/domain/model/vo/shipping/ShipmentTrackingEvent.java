package shopping.international.domain.model.vo.shipping;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
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
 * 物流轨迹事件值对象
 *
 * <p>用于驱动 {@code Shipment.applyTrackingEvent(...)} 的统一入参</p>
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ShipmentTrackingEvent implements Verifiable {

    /**
     * 目标状态, 为空表示仅记录日志不推进状态
     */
    @Nullable
    private final ShipmentStatus toStatus;
    /**
     * 事件发生时间, 为空时由聚合回落为当前时间
     */
    @Nullable
    private final LocalDateTime eventTime;
    /**
     * 事件来源类型
     */
    private final ShipmentStatusEventSource sourceType;
    /**
     * 来源引用标识, 用于幂等去重
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
     * 备注信息
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
    private final String rawPayloadText;
    /**
     * 操作者用户 ID
     */
    @Nullable
    private final Long actorUserId;

    /**
     * 构造物流轨迹事件
     *
     * @param toStatus    目标状态
     * @param eventTime   事件时间
     * @param sourceType  来源类型
     * @param sourceRef   来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo  追踪号
     * @param note        备注
     * @param rawPayload  原始报文
     * @param rawPayloadText 原始报文文本
     * @param actorUserId 操作者用户 ID
     */
    private ShipmentTrackingEvent(@Nullable ShipmentStatus toStatus,
                                  @Nullable LocalDateTime eventTime,
                                  ShipmentStatusEventSource sourceType,
                                  String sourceRef,
                                  @Nullable String carrierCode,
                                  @Nullable String trackingNo,
                                  @Nullable String note,
                                  @Nullable Map<String, Object> rawPayload,
                                  @Nullable String rawPayloadText,
                                  @Nullable Long actorUserId) {
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
    }

    /**
     * 创建状态推进事件
     *
     * @param toStatus    目标状态
     * @param eventTime   事件时间
     * @param sourceType  来源类型
     * @param sourceRef   来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo  追踪号
     * @param note        备注
     * @param rawPayload  原始报文
     * @param actorUserId 操作者用户 ID
     * @return 物流轨迹事件值对象
     */
    public static ShipmentTrackingEvent transition(ShipmentStatus toStatus,
                                                   @Nullable LocalDateTime eventTime,
                                                   ShipmentStatusEventSource sourceType,
                                                   String sourceRef,
                                                   @Nullable String carrierCode,
                                                   @Nullable String trackingNo,
                                                   @Nullable String note,
                                                   @Nullable Map<String, Object> rawPayload,
                                                   @Nullable Long actorUserId) {
        return transition(
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
     * 创建状态推进事件
     *
     * @param toStatus    目标状态
     * @param eventTime   事件时间
     * @param sourceType  来源类型
     * @param sourceRef   来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo  追踪号
     * @param note        备注
     * @param rawPayload  原始报文
     * @param rawPayloadText 原始报文文本
     * @param actorUserId 操作者用户 ID
     * @return 物流轨迹事件值对象
     */
    public static ShipmentTrackingEvent transition(ShipmentStatus toStatus,
                                                   @Nullable LocalDateTime eventTime,
                                                   ShipmentStatusEventSource sourceType,
                                                   String sourceRef,
                                                   @Nullable String carrierCode,
                                                   @Nullable String trackingNo,
                                                   @Nullable String note,
                                                   @Nullable Map<String, Object> rawPayload,
                                                   @Nullable String rawPayloadText,
                                                   @Nullable Long actorUserId) {
        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                toStatus, eventTime, sourceType, sourceRef,
                carrierCode, trackingNo, note, rawPayload, rawPayloadText, actorUserId
        );
        event.validate();
        return event;
    }

    /**
     * 创建仅记录日志的事件
     *
     * @param eventTime   事件时间
     * @param sourceType  来源类型
     * @param sourceRef   来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo  追踪号
     * @param note        备注
     * @param rawPayload  原始报文
     * @param actorUserId 操作者用户 ID
     * @return 物流轨迹事件值对象
     */
    public static ShipmentTrackingEvent keepCurrent(@Nullable LocalDateTime eventTime,
                                                    ShipmentStatusEventSource sourceType,
                                                    String sourceRef,
                                                    @Nullable String carrierCode,
                                                    @Nullable String trackingNo,
                                                    @Nullable String note,
                                                    @Nullable Map<String, Object> rawPayload,
                                                    @Nullable Long actorUserId) {
        return keepCurrent(
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
     * 创建仅记录日志的事件
     *
     * @param eventTime   事件时间
     * @param sourceType  来源类型
     * @param sourceRef   来源引用
     * @param carrierCode 承运商编码
     * @param trackingNo  追踪号
     * @param note        备注
     * @param rawPayload  原始报文
     * @param rawPayloadText 原始报文文本
     * @param actorUserId 操作者用户 ID
     * @return 物流轨迹事件值对象
     */
    public static ShipmentTrackingEvent keepCurrent(@Nullable LocalDateTime eventTime,
                                                    ShipmentStatusEventSource sourceType,
                                                    String sourceRef,
                                                    @Nullable String carrierCode,
                                                    @Nullable String trackingNo,
                                                    @Nullable String note,
                                                    @Nullable Map<String, Object> rawPayload,
                                                    @Nullable String rawPayloadText,
                                                    @Nullable Long actorUserId) {
        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                null, eventTime, sourceType, sourceRef,
                carrierCode, trackingNo, note, rawPayload, rawPayloadText, actorUserId);
        event.validate();
        return event;
    }

    /**
     * 校验并规范化轨迹事件字段
     */
    @Override
    public void validate() {
        require(sourceType != null, "sourceType 不能为空");
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

        if (rawPayload != null)
            rawPayload = Collections.unmodifiableMap(new LinkedHashMap<>(rawPayload));
        if (rawPayloadText != null)
            require(!rawPayloadText.isBlank(), "rawPayloadText 不能为空白");

        if (actorUserId != null)
            require(actorUserId > 0, "actorUserId 必须大于 0");
    }
}
