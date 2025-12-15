package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 取消订单请求体 (OrderCancelRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelRequest implements Verifiable {
    /**
     * 取消原因 (最大长度 255)
     */
    private String reason;

    /**
     * 校验并规范化字段
     */
    @Override
    public void validate() {
        reason = normalizeNotNullField(reason, "reason 不能为空", s -> s.length() <= 255, "reason 长度不能超过 255 个字符");
    }
}

