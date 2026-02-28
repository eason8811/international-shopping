package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.*;

/**
 * 管理侧工单指派请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketAssignRequest implements Verifiable {
    /**
     * 目标指派坐席用户 ID
     */
    @Nullable
    private Long toAssigneeUserId;
    /**
     * 指派动作类型
     */
    @Nullable
    private String actionType;
    /**
     * 操作备注
     */
    @Nullable
    private String note;
    /**
     * 来源引用 ID
     */
    @Nullable
    private String sourceRef;

    /**
     * 对管理侧工单指派参数进行校验与规范化
     */
    @Override
    public void validate() {
        actionType = normalizeNotNullField(actionType, "actionType 不能为空",
                AdminTicketAssignRequest::isActionType,
                "actionType 不支持").toUpperCase(Locale.ROOT);

        if (toAssigneeUserId != null)
            require(toAssigneeUserId >= 1, "toAssigneeUserId 必须大于等于 1");

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");

        sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空",
                value -> value.length() <= 128,
                "sourceRef 长度不能超过 128 个字符");

        if ("ASSIGN".equals(actionType) || "REASSIGN".equals(actionType) || "AUTO_ASSIGN".equals(actionType))
            requireNotNull(toAssigneeUserId, "当前 actionType 需要提供 toAssigneeUserId");
    }

    /**
     * 判断是否为受支持的指派动作
     *
     * @param value 原始动作
     * @return 若受支持则返回 {@code true}
     */
    private static boolean isActionType(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ASSIGN", "REASSIGN", "UNASSIGN", "AUTO_ASSIGN" -> true;
            default -> false;
        };
    }
}
