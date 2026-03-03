package shopping.international.trigger.controller.customerservice;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.api.resp.customerservice.ReshipDetailRespond;
import shopping.international.api.resp.customerservice.ReshipItemRespond;
import shopping.international.api.resp.customerservice.ReshipShipmentRespond;
import shopping.international.api.resp.customerservice.ReshipSummaryRespond;
import shopping.international.domain.model.aggregate.customerservice.AfterSalesReship;
import shopping.international.domain.model.entity.customerservice.ReshipItem;
import shopping.international.domain.model.entity.customerservice.ReshipShipment;

import java.util.List;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧补发单响应装配器, 负责聚合和实体到 DTO 的转换
 */
public final class AdminReshipRespondAssembler {

    /**
     * 私有构造方法, 工具类不允许实例化
     */
    private AdminReshipRespondAssembler() {
    }

    /**
     * 补发单聚合转换为摘要响应 DTO
     *
     * @param reship 补发单聚合
     * @return 摘要响应 DTO
     */
    public static @NotNull ReshipSummaryRespond toSummaryRespond(@NotNull AfterSalesReship reship) {
        return ReshipSummaryRespond.builder()
                .id(requireId(reship.getId(), "reship.id"))
                .reshipNo(reship.getReshipNo().getValue())
                .orderId(reship.getOrderId())
                .ticketId(reship.getTicketId())
                .reasonCode(reship.getReasonCode())
                .status(reship.getStatus())
                .currency(reship.getCurrency())
                .itemsCost(reship.getItemsCost())
                .shippingCost(reship.getShippingCost())
                .note(reship.getNote())
                .createdAt(reship.getCreatedAt())
                .updatedAt(reship.getUpdatedAt())
                .build();
    }

    /**
     * 补发单聚合转换为详情响应 DTO
     *
     * @param reship 补发单聚合
     * @return 详情响应 DTO
     */
    public static @NotNull ReshipDetailRespond toDetailRespond(@NotNull AfterSalesReship reship) {
        List<ReshipItemRespond> items = reship.getItemList().isEmpty()
                ? List.of()
                : reship.getItemList().stream().map(AdminReshipRespondAssembler::toItemRespond).toList();
        List<ReshipShipmentRespond> shipments = reship.getShipmentList().isEmpty()
                ? List.of()
                : reship.getShipmentList().stream().map(AdminReshipRespondAssembler::toShipmentRespond).toList();

        return ReshipDetailRespond.builder()
                .id(requireId(reship.getId(), "reship.id"))
                .reshipNo(reship.getReshipNo().getValue())
                .orderId(reship.getOrderId())
                .ticketId(reship.getTicketId())
                .reasonCode(reship.getReasonCode())
                .status(reship.getStatus())
                .currency(reship.getCurrency())
                .itemsCost(reship.getItemsCost())
                .shippingCost(reship.getShippingCost())
                .note(reship.getNote())
                .createdAt(reship.getCreatedAt())
                .updatedAt(reship.getUpdatedAt())
                .items(items)
                .shipments(shipments)
                .build();
    }

    /**
     * 补发明细实体转换为响应 DTO
     *
     * @param item 补发明细实体
     * @return 补发明细响应 DTO
     */
    public static @NotNull ReshipItemRespond toItemRespond(@NotNull ReshipItem item) {
        return ReshipItemRespond.builder()
                .id(requireId(item.getId(), "reshipItem.id"))
                .reshipId(requireId(item.getReshipId(), "reshipItem.reshipId"))
                .orderItemId(item.getOrderItemId())
                .skuId(item.getSkuId())
                .quantity(item.getQuantity())
                .build();
    }

    /**
     * 补发和物流单关联实体转换为响应 DTO
     *
     * @param shipment 关联实体
     * @return 关联响应 DTO
     */
    public static @NotNull ReshipShipmentRespond toShipmentRespond(@NotNull ReshipShipment shipment) {
        return ReshipShipmentRespond.builder()
                .reshipId(requireId(shipment.getReshipId(), "reshipShipment.reshipId"))
                .shipmentId(shipment.getShipmentId())
                .createdAt(shipment.getCreatedAt())
                .build();
    }

    /**
     * 校验 ID 非空并返回 ID
     *
     * @param value     ID 值
     * @param fieldName 字段名
     * @return 非空 ID 值
     */
    private static @NotNull Long requireId(@Nullable Long value, @NotNull String fieldName) {
        require(value != null && value > 0, fieldName + " 不能为空");
        return value;
    }
}
