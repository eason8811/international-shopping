package shopping.international.domain.model.vo.shipping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;

import java.time.LocalDateTime;

/**
 * 物流单摘要视图, 用于分页列表返回
 *
 * @param id 物流单主键
 * @param shipmentNo 物流单号
 * @param orderId 订单主键
 * @param orderNo 订单号
 * @param idempotencyKey 幂等键
 * @param carrierCode 承运商编码
 * @param carrierName 承运商名称
 * @param serviceCode 服务编码
 * @param trackingNo 追踪号
 * @param extExternalId 三方物流外部单号
 * @param status 物流状态
 * @param pickupTime 揽收时间
 * @param deliveredTime 签收时间
 * @param currency 币种
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ShipmentSummaryView(@NotNull Long id,
                                  @NotNull String shipmentNo,
                                  @Nullable Long orderId,
                                  @Nullable String orderNo,
                                  @Nullable String idempotencyKey,
                                  @Nullable String carrierCode,
                                  @Nullable String carrierName,
                                  @Nullable String serviceCode,
                                  @Nullable String trackingNo,
                                  @Nullable String extExternalId,
                                  @NotNull ShipmentStatus status,
                                  @Nullable LocalDateTime pickupTime,
                                  @Nullable LocalDateTime deliveredTime,
                                  @NotNull String currency,
                                  @NotNull LocalDateTime createdAt,
                                  @NotNull LocalDateTime updatedAt) {
}
