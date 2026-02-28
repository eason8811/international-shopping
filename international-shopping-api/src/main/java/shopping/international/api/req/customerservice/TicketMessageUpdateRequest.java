package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 工单消息编辑请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageUpdateRequest implements Verifiable {
    /**
     * 更新后的消息内容
     */
    @Nullable
    private String content;

    /**
     * 对工单消息编辑参数进行校验与规范化
     */
    @Override
    public void validate() {
        content = normalizeNotNullField(content, "content 不能为空",
                value -> value.length() <= 4000,
                "content 长度不能超过 4000 个字符");
    }
}
