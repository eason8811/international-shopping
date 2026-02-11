package shopping.international.api.resp.shipping;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 物流单摘要响应对象 (ShipmentSummaryRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentSummaryRespond {
    /**
     * 主键 ID
     */
    @Nullable
    private Long id;
    /**
     * 物流单号
     */
    @Nullable
    private String shipmentNo;
    /**
     * 订单 ID
     */
    @Nullable
    private Long orderId;
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;
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
     * 三方物流单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 当前物流状态
     */
    @Nullable
    private String status;
    /**
     * 揽收时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;
    /**
     * 签收时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredTime;
    /**
     * 币种
     */
    @Nullable
    private String currency;
    /**
     * 创建时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
