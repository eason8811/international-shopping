package shopping.international.domain.service.shipping.impl;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import shopping.international.domain.adapter.port.shipping.ISeventeenTrackPort;
import shopping.international.domain.adapter.repository.shipping.IShipmentRepository;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.enums.shipping.SeventeenTrackSubStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.domain.model.vo.shipping.ShipmentTrackingEvent;
import shopping.international.domain.service.shipping.IShipmentWebhookService;
import shopping.international.types.config.SeventeenTrackProperties;
import shopping.international.types.exceptions.ConflictException;
import shopping.international.types.exceptions.IllegalParamException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import static shopping.international.types.utils.FieldValidateUtils.nestedString;
import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * 物流 WebHook 领域服务实现
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(SeventeenTrackProperties.class)
public class ShipmentWebhookService implements IShipmentWebhookService {

    /**
     * 物流仓储
     */
    private final IShipmentRepository shipmentRepository;
    /**
     * 17Track 端口
     */
    private final ISeventeenTrackPort seventeenTrackPort;
    /**
     * 17 Track 配置属性
     */
    private final SeventeenTrackProperties properties;

    /**
     * 处理 17Track WebHook 回调
     *
     * @param signHeader 签名头
     * @param rawBody    原始请求体
     * @param payload    解析后的请求体
     */
    @Override
    public void handleSeventeenTrackWebhook(@NotNull String signHeader,
                                            @NotNull String rawBody,
                                            @NotNull Map<String, Object> payload) {
        Duration replayTtl = resolveReplayTtl();
        String dedupeKey = "17track:webhook:" + sha256(rawBody);
        ISeventeenTrackPort.WebhookGateResult gateResult = seventeenTrackPort.verifyWebhookAndTryEnterProcessing(
                new ISeventeenTrackPort.VerifyWebhookCommand(signHeader, rawBody, dedupeKey, replayTtl)
        );
        if (gateResult == ISeventeenTrackPort.WebhookGateResult.ALREADY_PROCESSED)
            return;
        if (gateResult == ISeventeenTrackPort.WebhookGateResult.PROCESSING)
            throw new ConflictException("17Track WebHook 事件处理中, 请重试");

        try {
            List<Map<String, Object>> eventDataList = extractEventDataList(payload);
            if (eventDataList.isEmpty()) {
                seventeenTrackPort.markWebhookProcessed(dedupeKey, replayTtl);
                return;
            }

            String trackingNo = nestedString(payload, "data", "number");
            String sourceRef = "17track:" + sha256(rawBody);

            if (trackingNo == null || trackingNo.isBlank())
                throw new IllegalParamException("WebHook 回调中缺失物流单号");
            Optional<Shipment> shipmentOptional = shipmentRepository.findShipmentDetailByTrackingNo(trackingNo, true);
            if (shipmentOptional.isEmpty() || shipmentOptional.get().getId() == null) {
                seventeenTrackPort.markWebhookProcessed(dedupeKey, replayTtl);
                return;
            }

            Shipment shipment = shipmentOptional.get();
            String subStatus = nestedString(payload, "data", "track_info", "latest_status", "sub_status");
            LocalDateTime eventTime = LocalDateTime.now();
            String iso = nestedString(payload, "data", "track_info", "latest_event", "time_iso");
            if (iso != null && !iso.isBlank())
                eventTime = OffsetDateTime.parse(iso.strip()).toLocalDateTime();
            String carrierCode = nestedString(payload, "data", "carrier");
            ShipmentTrackingEvent event = buildTrackingEvent(
                    shipment,
                    subStatus,
                    trackingNo,
                    eventTime,
                    sourceRef,
                    carrierCode,
                    rawBody,
                    payload
            );
            shipmentRepository.applyTrackingEvent(shipment.getId(), event);
            seventeenTrackPort.markWebhookProcessed(dedupeKey, replayTtl);
        } catch (Exception exception) {
            seventeenTrackPort.clearWebhookProcessing(dedupeKey);
            throw exception;
        }
    }

    /**
     * 解析 WebHook 重放保护 TTL
     *
     * @return 重放保护 TTL
     */
    private @NotNull Duration resolveReplayTtl() {
        if (properties.getReplayTtl() == null || properties.getReplayTtl().isNegative() || properties.getReplayTtl().isZero())
            return Duration.ofDays(4);
        return properties.getReplayTtl();
    }

    /**
     * 构造轨迹事件
     *
     * @param shipment    当前物流单
     * @param subStatus   17Track 子状态
     * @param trackingNo  追踪号
     * @param eventTime   事件时间
     * @param sourceRef   来源引用
     * @param carrierCode 运输商代码
     * @param rawBody     原始请求体
     * @param payload     原始报文
     * @return 轨迹事件
     */
    private @NotNull ShipmentTrackingEvent buildTrackingEvent(@NotNull Shipment shipment,
                                                              @Nullable String subStatus,
                                                              @NotNull String trackingNo,
                                                              @Nullable LocalDateTime eventTime,
                                                              @NotNull String sourceRef,
                                                              @Nullable String carrierCode,
                                                              @NotNull String rawBody,
                                                              @NotNull Map<String, Object> payload) {
        String note = subStatus == null || subStatus.isBlank()
                ? "17Track 未返回 sub_status, 仅记录日志"
                : "17Track sub_status: " + subStatus;

        if (subStatus == null || subStatus.isBlank()) {
            return ShipmentTrackingEvent.keepCurrent(
                    eventTime,
                    ShipmentStatusEventSource.CARRIER_WEBHOOK,
                    sourceRef,
                    carrierCode,
                    trackingNo,
                    note,
                    payload,
                    rawBody,
                    null
            );
        }

        Optional<SeventeenTrackSubStatus> mapped = SeventeenTrackSubStatus.resolve(subStatus);
        if (mapped.isEmpty()) {
            return ShipmentTrackingEvent.keepCurrent(
                    eventTime,
                    ShipmentStatusEventSource.CARRIER_WEBHOOK,
                    sourceRef,
                    carrierCode,
                    trackingNo,
                    note,
                    payload,
                    rawBody,
                    null
            );
        }

        SeventeenTrackSubStatus subStatusEnum = mapped.get();
        if (subStatusEnum.keepCurrent()) {
            return ShipmentTrackingEvent.keepCurrent(
                    eventTime,
                    ShipmentStatusEventSource.CARRIER_WEBHOOK,
                    sourceRef,
                    carrierCode,
                    trackingNo,
                    note,
                    payload,
                    rawBody,
                    null
            );
        }

        if (subStatusEnum.isNotFoundInvalidCode()
                && shipment.getStatus() != ShipmentStatus.CREATED
                && shipment.getStatus() != ShipmentStatus.LABEL_CREATED) {
            return ShipmentTrackingEvent.keepCurrent(
                    eventTime,
                    ShipmentStatusEventSource.CARRIER_WEBHOOK,
                    sourceRef,
                    carrierCode,
                    trackingNo,
                    note,
                    payload,
                    rawBody,
                    null
            );
        }

        return ShipmentTrackingEvent.transition(
                subStatusEnum.targetStatus(),
                eventTime,
                ShipmentStatusEventSource.CARRIER_WEBHOOK,
                sourceRef,
                carrierCode,
                trackingNo,
                note,
                payload,
                rawBody,
                null
        );
    }

    /**
     * 提取 WebHook 中的事件数据列表
     *
     * @param payload WebHook 报文
     * @return 事件数据列表
     */
    private @NotNull List<Map<String, Object>> extractEventDataList(@NotNull Map<String, Object> payload) {
        Map<String, Object> dataMap = castToStringObjectMap(payload.get("data"));
        if (dataMap == null)
            return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        Object accepted = dataMap.get("accepted");
        if (accepted instanceof List<?> acceptedList)
            for (Object item : acceptedList) {
                Map<String, Object> acceptedItem = castToStringObjectMap(item);
                if (acceptedItem != null)
                    result.add(acceptedItem);
            }
        if (!result.isEmpty())
            return List.copyOf(result);

        result.add(dataMap);
        return List.copyOf(result);
    }

    /**
     * 将任意对象安全转换为 String,Object 结构的 Map
     *
     * @param raw 任意对象
     * @return 转换后的 Map, 无法转换时返回 null
     */
    private @Nullable Map<String, Object> castToStringObjectMap(@Nullable Object raw) {
        if (!(raw instanceof Map<?, ?> rawMap))
            return null;
        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key)
                converted.put(key, entry.getValue());
        }
        return converted;
    }

    /**
     * 计算 SHA256
     *
     * @param raw 原文
     * @return SHA256 字符串
     */
    private static @NotNull String sha256(@NotNull String raw) {
        requireNotBlank(raw, "raw 不能为空");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("计算 SHA256 失败, " + exception.getMessage(), exception);
        }
    }
}
