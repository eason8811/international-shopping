package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧物流单详情查询请求对象 (AdminShipmentDetailRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShipmentDetailRequest implements Verifiable {
    /**
     * 物流单主键 ID
     */
    @Nullable
    private Long shipmentId;

    /**
     * 对物流单详情查询参数进行校验
     */
    @Override
    public void validate() {
        requireNotNull(shipmentId, "shipmentId 不能为空");
        require(shipmentId >= 1, "shipmentId 必须大于等于 1");
    }
}
