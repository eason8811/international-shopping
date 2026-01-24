package shopping.international.api.req.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

/**
 * Capture PayPal 支付请求体 (回跳后确认扣款) 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayPalCaptureRequest implements Verifiable {

    /**
     * 回跳携带的 payer_id (如有)
     */
    @Nullable
    private String payerId;

    /**
     * 备注信息
     */
    @Nullable
    private String note;

    /**
     * 基本参数校验与字段归一化
     */
    @Override
    public void validate() {
        payerId = normalizeNullableField(payerId, "payerId 不能为空",
                s -> s.length() <= 128, "payerId 长度不能超过 128 个字符");
        note = normalizeNullableField(note, "note 不能为空",
                s -> s.length() <= 500, "note 长度不能超过 500 个字符");
    }
}

