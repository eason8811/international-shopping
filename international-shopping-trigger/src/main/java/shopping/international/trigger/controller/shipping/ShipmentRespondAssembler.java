package shopping.international.trigger.controller.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.api.resp.shipping.*;
import shopping.international.domain.model.aggregate.shipping.Shipment;
import shopping.international.domain.model.entity.shipping.ShipmentItem;
import shopping.international.domain.model.entity.shipping.ShipmentStatusLog;
import shopping.international.domain.model.vo.shipping.CustomsInfoSnapshot;
import shopping.international.domain.model.vo.shipping.ShipmentSummaryView;
import shopping.international.domain.model.vo.shipping.ShippingAddressSnapshot;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 物流响应组装器, 负责 shipping 领域对象到响应 DTO 的转换
 */
public final class ShipmentRespondAssembler {

    /**
     * 私有构造函数, 禁止实例化
     */
    private ShipmentRespondAssembler() {
    }

    /**
     * 将摘要视图转换为响应对象
     *
     * @param view 摘要视图
     * @return 摘要响应对象
     */
    public static @NotNull ShipmentSummaryRespond toSummaryRespond(@NotNull ShipmentSummaryView view) {
        return ShipmentSummaryRespond.builder()
                .id(view.id())
                .shipmentNo(view.shipmentNo())
                .orderId(view.orderId())
                .orderNo(view.orderNo())
                .idempotencyKey(view.idempotencyKey())
                .carrierCode(view.carrierCode())
                .carrierName(view.carrierName())
                .serviceCode(view.serviceCode())
                .trackingNo(view.trackingNo())
                .extExternalId(view.extExternalId())
                .status(view.status().name())
                .pickupTime(view.pickupTime())
                .deliveredTime(view.deliveredTime())
                .currency(view.currency())
                .createdAt(view.createdAt())
                .updatedAt(view.updatedAt())
                .build();
    }

    /**
     * 将物流聚合转换为响应对象
     *
     * @param shipment 物流聚合
     * @return 详情响应对象
     */
    public static @NotNull ShipmentDetailRespond toShipmentDetailRespond(@NotNull Shipment shipment) {
        List<ShipmentItemRespond> itemResponds = shipment.getItemList().stream()
                .map(ShipmentRespondAssembler::toItemRespond)
                .toList();
        List<ShipmentStatusLogRespond> statusLogResponds = shipment.getStatusLogList().stream()
                .map(ShipmentRespondAssembler::toStatusLogRespond)
                .toList();

        return ShipmentDetailRespond.builder()
                .id(shipment.getId())
                .shipmentNo(shipment.getShipmentNo().getValue())
                .orderId(shipment.getOrderId())
                .orderNo(shipment.getOrderNo())
                .idempotencyKey(shipment.getIdempotencyKey())
                .carrierCode(shipment.getCarrierCode())
                .carrierName(shipment.getCarrierName())
                .serviceCode(shipment.getServiceCode())
                .trackingNo(shipment.getTrackingNo())
                .extExternalId(shipment.getExtExternalId())
                .status(shipment.getStatus().name())
                .shipFrom(toAddressRespond(shipment.getShipFrom()))
                .shipTo(toAddressRespond(shipment.getShipTo()))
                .weightKg(toPlainDecimal(shipment.getDimension() == null ? null : shipment.getDimension().getWeightKg()))
                .lengthCm(toPlainDecimal(shipment.getDimension() == null ? null : shipment.getDimension().getLengthCm()))
                .widthCm(toPlainDecimal(shipment.getDimension() == null ? null : shipment.getDimension().getWidthCm()))
                .heightCm(toPlainDecimal(shipment.getDimension() == null ? null : shipment.getDimension().getHeightCm()))
                .declaredValue(shipment.getDeclaredValue())
                .currency(shipment.getCurrency())
                .customsInfo(toCustomsRespond(shipment.getCustomsInfo()))
                .labelUrl(shipment.getLabelUrl())
                .pickupTime(shipment.getPickupTime())
                .deliveredTime(shipment.getDeliveredTime())
                .addressChangeAllowed(shipment.isAddressChangeAllowed())
                .items(itemResponds)
                .statusLogs(statusLogResponds)
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }

    /**
     * 将物流明细实体转换为响应对象
     *
     * @param item 物流明细实体
     * @return 物流明细响应对象
     */
    public static @NotNull ShipmentItemRespond toItemRespond(@NotNull ShipmentItem item) {
        return ShipmentItemRespond.builder()
                .id(item.getId())
                .shipmentId(item.getShipmentId())
                .orderId(item.getOrderId())
                .orderItemId(item.getOrderItemId())
                .productId(item.getProductId())
                .skuId(item.getSkuId())
                .quantity(item.getQuantity())
                .createdAt(item.getCreatedAt())
                .build();
    }

    /**
     * 将状态日志实体转换为响应对象
     *
     * @param log 状态日志实体
     * @return 状态日志响应对象
     */
    public static @NotNull ShipmentStatusLogRespond toStatusLogRespond(@NotNull ShipmentStatusLog log) {
        return ShipmentStatusLogRespond.builder()
                .id(log.getId())
                .shipmentId(log.getShipmentId())
                .fromStatus(log.getFromStatus() == null ? null : log.getFromStatus().name())
                .toStatus(log.getToStatus().name())
                .eventTime(log.getEventTime())
                .sourceType(log.getSourceType().name())
                .sourceRef(log.getSourceRef())
                .carrierCode(log.getCarrierCode())
                .trackingNo(log.getTrackingNo())
                .note(log.getNote())
                .rawPayload(log.getRawPayload())
                .actorUserId(log.getActorUserId())
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * 将地址快照值对象转换为响应对象
     *
     * @param snapshot 地址快照值对象
     * @return 地址快照响应对象
     */
    public static @Nullable AddressSnapshotRespond toAddressRespond(@Nullable ShippingAddressSnapshot snapshot) {
        if (snapshot == null)
            return null;
        return AddressSnapshotRespond.builder()
                .receiverName(snapshot.getReceiverName())
                .phone(snapshot.getPhone())
                .country(snapshot.getCountry())
                .province(snapshot.getProvince())
                .city(snapshot.getCity())
                .district(snapshot.getDistrict())
                .addressLine1(snapshot.getAddressLine1())
                .addressLine2(snapshot.getAddressLine2())
                .zipcode(snapshot.getZipcode())
                .build();
    }

    /**
     * 将关务快照值对象转换为响应对象
     *
     * @param customsInfo 关务快照值对象
     * @return 关务响应对象
     */
    public static @Nullable CustomsInfoSnapshotRespond toCustomsRespond(@Nullable CustomsInfoSnapshot customsInfo) {
        if (customsInfo == null)
            return null;
        return CustomsInfoSnapshotRespond.builder()
                .extra(new LinkedHashMap<>(customsInfo.getExtra()))
                .build();
    }

    /**
     * 将十进制数值转换为普通字符串
     *
     * @param value 十进制数值
     * @return 普通字符串, 为空时返回 null
     */
    private static @Nullable String toPlainDecimal(@Nullable BigDecimal value) {
        if (value == null)
            return null;
        return value.stripTrailingZeros().toPlainString();
    }
}
