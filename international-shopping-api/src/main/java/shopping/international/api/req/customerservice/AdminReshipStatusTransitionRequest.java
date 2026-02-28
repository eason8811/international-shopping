package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.ReshipStatus;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

/**
 * 管理侧补发单状态流转请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReshipStatusTransitionRequest implements Verifiable {
    /**
     * 目标补发状态
     */
    @Nullable
    private ReshipStatus toStatus;
    /**
     * 备注信息
     */
    @Nullable
    private String note;

    /**
     * 对补发单状态流转参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(toStatus, "toStatus 不能为空");

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");
    }
}
