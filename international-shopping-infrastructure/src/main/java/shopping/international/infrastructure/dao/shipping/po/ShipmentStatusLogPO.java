package shopping.international.infrastructure.dao.shipping.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 持久化对象, shipment_status_log 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"createdAt"})
@TableName("shipment_status_log")
public class ShipmentStatusLogPO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 物流单主键
     */
    @TableField("shipment_id")
    private Long shipmentId;
    /**
     * 变更前状态
     */
    @TableField("from_status")
    private String fromStatus;
    /**
     * 变更后状态
     */
    @TableField("to_status")
    private String toStatus;
    /**
     * 事件时间
     */
    @TableField("event_time")
    private LocalDateTime eventTime;
    /**
     * 来源类型
     */
    @TableField("source_type")
    private String sourceType;
    /**
     * 来源引用
     */
    @TableField("source_ref")
    private String sourceRef;
    /**
     * 承运商编码
     */
    @TableField("carrier_code")
    private String carrierCode;
    /**
     * 追踪号
     */
    @TableField("tracking_no")
    private String trackingNo;
    /**
     * 备注
     */
    @TableField("note")
    private String note;
    /**
     * 原始报文 JSON
     */
    @TableField("raw_payload")
    private String rawPayload;
    /**
     * 操作者主键
     */
    @TableField("actor_user_id")
    private Long actorUserId;
    /**
     * 创建时间
     */
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
