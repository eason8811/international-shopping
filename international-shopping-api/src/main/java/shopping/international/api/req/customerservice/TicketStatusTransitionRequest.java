package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketStatus;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 工单状态流转请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusTransitionRequest implements Verifiable {
    /**
     * 目标状态
     */
    @Nullable
    private TicketStatus toStatus;
    /**
     * 备注信息
     */
    @Nullable
    private String note;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;

    /**
     * 对工单状态流转参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(toStatus, "toStatus 不能为空");

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");

        sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空",
                value -> value.length() <= 128,
                "sourceRef 长度不能超过 128 个字符");
    }
}
