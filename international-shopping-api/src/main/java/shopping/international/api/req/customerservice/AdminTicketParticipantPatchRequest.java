package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;

/**
 * 管理侧工单参与方角色更新请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketParticipantPatchRequest implements Verifiable {
    /**
     * 参与方角色
     */
    @Nullable
    private String role;

    /**
     * 对工单参与方角色更新参数进行校验与规范化
     */
    @Override
    public void validate() {
        role = normalizeNotNullField(role, "role 不能为空",
                AdminTicketParticipantPatchRequest::isParticipantRole,
                "role 不支持").toUpperCase(Locale.ROOT);
    }

    /**
     * 判断是否为受支持的参与方角色
     *
     * @param value 原始角色
     * @return 若受支持则返回 {@code true}
     */
    private static boolean isParticipantRole(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OWNER", "ASSIGNEE", "COLLABORATOR", "WATCHER" -> true;
            default -> false;
        };
    }
}
