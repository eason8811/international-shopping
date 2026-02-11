package shopping.international.api.req.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 用户侧订单物流列表查询请求对象 (UserOrderShipmentListRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderShipmentListRequest implements Verifiable {
    /**
     * 订单号
     */
    @Nullable
    private String orderNo;

    /**
     * 是否返回状态日志，默认返回
     */
    @Nullable
    private Boolean includeLogs = true;

    /**
     * 对订单物流列表查询参数进行校验与规范化
     */
    @Override
    public void validate() {
        orderNo = normalizeNotNullField(orderNo, "orderNo 不能为空",
                s -> s.length() >= 10 && s.length() <= 32,
                "orderNo 长度需在 10~32 个字符之间");

        if (includeLogs == null)
            includeLogs = true;
    }
}
