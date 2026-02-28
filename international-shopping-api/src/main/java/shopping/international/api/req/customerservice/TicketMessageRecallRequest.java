package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

/**
 * 工单消息撤回请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageRecallRequest implements Verifiable {
    /**
     * 撤回原因
     */
    @Nullable
    private String reason;

    /**
     * 对工单消息撤回参数进行校验与规范化
     */
    @Override
    public void validate() {
        reason = normalizeNullableField(reason, "reason 不能为空",
                value -> value.length() <= 255,
                "reason 长度不能超过 255 个字符");
    }
}
