package shopping.international.api.resp.shipping;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 物流状态日志响应对象 (ShipmentStatusLogRespond)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusLogRespond {
    /**
     * 主键 ID
     */
    @Nullable
    private Long id;
    /**
     * 物流单 ID
     */
    @Nullable
    private Long shipmentId;
    /**
     * 变更前状态
     */
    @Nullable
    private String fromStatus;
    /**
     * 变更后状态
     */
    @Nullable
    private String toStatus;
    /**
     * 事件发生时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;
    /**
     * 事件来源类型
     */
    @Nullable
    private String sourceType;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;
    /**
     * 承运商编码
     */
    @Nullable
    private String carrierCode;
    /**
     * 追踪号
     */
    @Nullable
    private String trackingNo;
    /**
     * 备注
     */
    @Nullable
    private String note;
    /**
     * 原始报文
     */
    @Nullable
    private Map<String, Object> rawPayload;
    /**
     * 操作者用户 ID
     */
    @Nullable
    private Long actorUserId;
    /**
     * 记录创建时间
     */
    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
