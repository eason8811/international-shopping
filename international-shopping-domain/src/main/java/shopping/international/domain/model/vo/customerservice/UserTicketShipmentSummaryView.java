package shopping.international.domain.model.vo.customerservice;

import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;

import java.time.LocalDateTime;

/**
 * 用户侧工单关联物流摘要视图值对象
 *
 * @param id             物流单 ID
 * @param shipmentNo     物流单号
 * @param orderId        订单 ID
 * @param orderNo        订单号
 * @param idempotencyKey 幂等键
 * @param carrierCode    承运商编码
 * @param carrierName    承运商名称
 * @param serviceCode    服务编码
 * @param trackingNo     追踪号
 * @param extExternalId  外部单号
 * @param status         物流状态
 * @param pickupTime     揽收时间
 * @param deliveredTime  签收时间
 * @param currency       币种
 * @param createdAt      创建时间
 * @param updatedAt      更新时间
 */
public record UserTicketShipmentSummaryView(Long id,
                                            String shipmentNo,
                                            @Nullable Long orderId,
                                            @Nullable String orderNo,
                                            @Nullable String idempotencyKey,
                                            @Nullable String carrierCode,
                                            @Nullable String carrierName,
                                            @Nullable String serviceCode,
                                            @Nullable String trackingNo,
                                            @Nullable String extExternalId,
                                            ShipmentStatus status,
                                            @Nullable LocalDateTime pickupTime,
                                            @Nullable LocalDateTime deliveredTime,
                                            String currency,
                                            LocalDateTime createdAt,
                                            LocalDateTime updatedAt) {
}

