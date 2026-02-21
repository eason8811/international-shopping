package shopping.international.infrastructure.dao.shipping.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量发货状态 CAS 更新参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentDispatchStatusCasPO {

    /**
     * 物流单主键
     */
    private Long shipmentId;
    /**
     * 预期旧状态
     */
    private String oldStatus;
    /**
     * 目标新状态
     */
    private String newStatus;
    /**
     * 闸门来源类型
     */
    private String sourceType;
    /**
     * 闸门来源引用
     */
    private String sourceRef;
}
