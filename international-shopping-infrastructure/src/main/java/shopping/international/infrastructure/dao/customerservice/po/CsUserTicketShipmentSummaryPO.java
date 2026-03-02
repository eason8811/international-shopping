package shopping.international.infrastructure.dao.customerservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户侧工单关联物流摘要投影对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsUserTicketShipmentSummaryPO {

    /**
     * 物流单 ID
     */
    private Long id;
    /**
     * 物流单号
     */
    private String shipmentNo;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 幂等键
     */
    private String idempotencyKey;
    /**
     * 承运商编码
     */
    private String carrierCode;
    /**
     * 承运商名称
     */
    private String carrierName;
    /**
     * 服务编码
     */
    private String serviceCode;
    /**
     * 追踪号
     */
    private String trackingNo;
    /**
     * 外部单号
     */
    private String extExternalId;
    /**
     * 物流状态
     */
    private String status;
    /**
     * 揽收时间
     */
    private LocalDateTime pickupTime;
    /**
     * 签收时间
     */
    private LocalDateTime deliveredTime;
    /**
     * 币种
     */
    private String currency;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

