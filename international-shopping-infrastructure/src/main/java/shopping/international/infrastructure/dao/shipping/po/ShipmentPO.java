package shopping.international.infrastructure.dao.shipping.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 持久化对象, shipment 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt", "updatedAt", "items", "statusLogs"})
@TableName("shipment")
public class ShipmentPO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 物流单号
     */
    @TableField("shipment_no")
    private String shipmentNo;
    /**
     * 订单主键
     */
    @TableField("order_id")
    private Long orderId;
    /**
     * 幂等键
     */
    @TableField("idempotency_key")
    private String idempotencyKey;
    /**
     * 承运商编码
     */
    @TableField("carrier_code")
    private String carrierCode;
    /**
     * 承运商名称
     */
    @TableField("carrier_name")
    private String carrierName;
    /**
     * 服务编码
     */
    @TableField("service_code")
    private String serviceCode;
    /**
     * 追踪号
     */
    @TableField("tracking_no")
    private String trackingNo;
    /**
     * 外部单号
     */
    @TableField("ext_external_id")
    private String extExternalId;
    /**
     * 状态
     */
    @TableField("status")
    private String status;
    /**
     * 发货地址快照 JSON
     */
    @TableField("ship_from")
    private String shipFrom;
    /**
     * 收货地址快照 JSON
     */
    @TableField("ship_to")
    private String shipTo;
    /**
     * 重量
     */
    @TableField("weight_kg")
    private BigDecimal weightKg;
    /**
     * 长度
     */
    @TableField("length_cm")
    private BigDecimal lengthCm;
    /**
     * 宽度
     */
    @TableField("width_cm")
    private BigDecimal widthCm;
    /**
     * 高度
     */
    @TableField("height_cm")
    private BigDecimal heightCm;
    /**
     * 申报价值
     */
    @TableField("declared_value")
    private Long declaredValue;
    /**
     * 币种
     */
    @TableField("currency")
    private String currency;
    /**
     * 关务 JSON
     */
    @TableField("customs_info")
    private String customsInfo;
    /**
     * 面单地址
     */
    @TableField("label_url")
    private String labelUrl;
    /**
     * 揽收时间
     */
    @TableField("pickup_time")
    private LocalDateTime pickupTime;
    /**
     * 签收时间
     */
    @TableField("delivered_time")
    private LocalDateTime deliveredTime;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
    /**
     * 订单号, 联表字段
     */
    @TableField(exist = false)
    private String orderNo;
    /**
     * 物流明细列表, 联表字段
     */
    @TableField(exist = false)
    private List<ShipmentItemPO> items;
    /**
     * 状态日志列表, 联表字段
     */
    @TableField(exist = false)
    private List<ShipmentStatusLogPO> statusLogs;
}
