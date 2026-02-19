package shopping.international.domain.model.vo.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.shipping.ShipmentStatus;
import shopping.international.domain.model.enums.shipping.ShipmentStatusEventSource;
import shopping.international.types.utils.Verifiable;

import java.time.LocalDateTime;

import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 物流状态日志分页查询条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentStatusLogPageCriteria implements Verifiable {

    /**
     * 物流单主键
     */
    @Nullable
    private Long shipmentId;
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;
    /**
     * 起始状态
     */
    @Nullable
    private ShipmentStatus fromStatus;
    /**
     * 目标状态
     */
    @Nullable
    private ShipmentStatus toStatus;
    /**
     * 事件来源
     */
    @Nullable
    private ShipmentStatusEventSource sourceType;
    /**
     * 来源引用
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
     * 事件时间起始
     */
    @Nullable
    private LocalDateTime eventTimeFrom;
    /**
     * 事件时间结束
     */
    @Nullable
    private LocalDateTime eventTimeTo;
    /**
     * 创建时间起始
     */
    @Nullable
    private LocalDateTime createdFrom;
    /**
     * 创建时间结束
     */
    @Nullable
    private LocalDateTime createdTo;
    /**
     * 排序字段
     */
    @Builder.Default
    private String sortField = "created_at";
    /**
     * 排序方向
     */
    @Builder.Default
    private String sortDirection = "desc";

    /**
     * 校验并规范化查询条件
     */
    @Override
    public void validate() {
        if (shipmentId != null)
            require(shipmentId > 0, "shipmentId 必须大于 0");
        if (eventTimeFrom != null && eventTimeTo != null)
            require(!eventTimeFrom.isAfter(eventTimeTo), "eventTimeFrom 不能晚于 eventTimeTo");
        if (createdFrom != null && createdTo != null)
            require(!createdFrom.isAfter(createdTo), "createdFrom 不能晚于 createdTo");

        if (sortField == null || sortField.isBlank())
            sortField = "created_at";
        else
            sortField = sortField.strip().toLowerCase();
        require("created_at".equals(sortField) || "event_time".equals(sortField) || "id".equals(sortField),
                "sortField 仅支持 created_at, event_time, id");

        if (sortDirection == null || sortDirection.isBlank())
            sortDirection = "desc";
        else
            sortDirection = sortDirection.strip().toLowerCase();
        require("asc".equals(sortDirection) || "desc".equals(sortDirection), "sortDirection 仅支持 asc 或 desc");
    }
}
