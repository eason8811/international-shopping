package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.normalizeNullableField;

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
    private String toStatus;
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
        toStatus = normalizeNotNullField(toStatus, "toStatus 不能为空",
                TicketStatusTransitionRequest::isTicketStatus,
                "toStatus 不支持").toUpperCase(Locale.ROOT);

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");

        sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空",
                value -> value.length() <= 128,
                "sourceRef 长度不能超过 128 个字符");
    }

    /**
     * 判断是否为受支持的工单状态
     *
     * @param value 原始状态
     * @return 若受支持则返回 {@code true}
     */
    private static boolean isTicketStatus(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OPEN", "IN_PROGRESS", "AWAITING_USER", "AWAITING_CARRIER", "ON_HOLD", "RESOLVED", "REJECTED", "CLOSED" ->
                    true;
            default -> false;
        };
    }
}
