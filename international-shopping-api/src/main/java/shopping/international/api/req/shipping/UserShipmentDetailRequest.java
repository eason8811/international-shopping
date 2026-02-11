package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 用户侧物流单详情查询请求对象 (UserShipmentDetailRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserShipmentDetailRequest implements Verifiable {
    /**
     * 物流单号
     */
    @Nullable
    private String shipmentNo;

    /**
     * 对物流单详情查询参数进行校验与规范化
     */
    @Override
    public void validate() {
        shipmentNo = normalizeNotNullField(shipmentNo, "shipmentNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "shipmentNo 长度需在 10~32 个字符之间");
    }
}
