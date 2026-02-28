package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketAssignmentActionType;
import shopping.international.types.utils.Verifiable;

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
    private TicketAssignmentActionType actionType;
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
        requireNotNull(actionType, "actionType 不能为空");

        if (toAssigneeUserId != null)
            require(toAssigneeUserId >= 1, "toAssigneeUserId 必须大于等于 1");

        note = normalizeNullableField(note, "note 不能为空",
                value -> value.length() <= 255,
                "note 长度不能超过 255 个字符");

        sourceRef = normalizeNullableField(sourceRef, "sourceRef 不能为空",
                value -> value.length() <= 128,
                "sourceRef 长度不能超过 128 个字符");

        if (actionType.requiresAssignee())
            requireNotNull(toAssigneeUserId, "当前 actionType 需要提供 toAssigneeUserId");
    }
}
