package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

/**
 * 工单关闭请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCloseRequest implements Verifiable {
    /**
     * 关闭备注
     */
    @Nullable
    private String note;

    /**
     * 对工单关闭参数进行校验与规范化
     */
    @Override
    public void validate() {
        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");
    }
}
