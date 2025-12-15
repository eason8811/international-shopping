package shopping.international.api.req.orders;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

/**
 * 管理侧确认退款请求体 (AdminConfirmRefundRequest)
 *
 * <p>用于管理侧确认退款时提交可选备注</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminConfirmRefundRequest implements Verifiable {
    /**
     * 备注信息 (可选, 最大长度 255)
     */
    @Nullable
    private String note;

    /**
     * 校验并规范化字段
     *
     * <p>若 {@code note} 非空, 则进行去首尾空白并校验最大长度</p>
     */
    @Override
    public void validate() {
        note = normalizeNullableField(note, "note 不能为空", s -> s.length() <= 255, "note 长度不能超过 255 个字符");
    }
}

