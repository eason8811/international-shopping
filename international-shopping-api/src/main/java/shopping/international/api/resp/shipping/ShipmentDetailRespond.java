package shopping.international.api.resp.shipping;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 物流单详情响应对象 (ShipmentDetailRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDetailRespond {
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
     * 三方物流外部单号
     */
    @Nullable
    private String extExternalId;
    /**
     * 当前物流状态
     */
    @Nullable
    private String status;
    /**
     * 发货地址快照
     */
    @Nullable
    private AddressSnapshotRespond shipFrom;
    /**
     * 收货地址快照
     */
    @Nullable
    private AddressSnapshotRespond shipTo;
    /**
     * 毛重（kg），最多 3 位小数
     */
    @Nullable
    private String weightKg;
    /**
     * 长（cm），最多 1 位小数
     */
    @Nullable
    private String lengthCm;
    /**
     * 宽（cm），最多 1 位小数
     */
    @Nullable
    private String widthCm;
    /**
     * 高（cm），最多 1 位小数
     */
    @Nullable
    private String heightCm;
    /**
     * 申报价值（最小货币单位）
     */
    @Nullable
    private Long declaredValue;
    /**
     * 币种
     */
    @Nullable
    private String currency;
    /**
     * 关务信息快照
     */
    @Nullable
    private CustomsInfoSnapshotRespond customsInfo;
    /**
     * 面单 URL
     */
    @Nullable
    private String labelUrl;
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
     * 是否允许改址
     */
    @Nullable
    private Boolean addressChangeAllowed;
    /**
     * 物流单商品明细
     */
    @Nullable
    private List<ShipmentItemRespond> items;
    /**
     * 物流状态日志列表
     */
    @Nullable
    private List<ShipmentStatusLogRespond> statusLogs;
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
