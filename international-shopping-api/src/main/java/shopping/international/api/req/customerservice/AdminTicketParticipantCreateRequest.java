package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.types.utils.Verifiable;

import java.util.Locale;

import static shopping.international.types.utils.FieldValidateUtils.normalizeNotNullField;
import static shopping.international.types.utils.FieldValidateUtils.require;

/**
 * 管理侧新增工单参与方请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTicketParticipantCreateRequest implements Verifiable {
    /**
     * 参与方类型
     */
    @Nullable
    private String participantType;
    /**
     * 参与用户 ID
     */
    @Nullable
    private Long participantUserId;
    /**
     * 参与方角色
     */
    @Nullable
    private String role;

    /**
     * 对新增工单参与方参数进行校验与规范化
     */
    @Override
    public void validate() {
        participantType = normalizeNotNullField(participantType, "participantType 不能为空",
                AdminTicketParticipantCreateRequest::isParticipantType,
                "participantType 不支持").toUpperCase(Locale.ROOT);

        if (participantUserId != null)
            require(participantUserId >= 1, "participantUserId 必须大于等于 1");

        role = normalizeNotNullField(role, "role 不能为空",
                AdminTicketParticipantCreateRequest::isParticipantRole,
                "role 不支持").toUpperCase(Locale.ROOT);

        if ("USER".equals(participantType) || "AGENT".equals(participantType) || "MERCHANT".equals(participantType))
            require(participantUserId != null, "当前 participantType 需要提供 participantUserId");
    }

    /**
     * 判断是否为受支持的参与方类型
     *
     * @param value 原始类型
     * @return 若受支持则返回 {@code true}
     */
    private static boolean isParticipantType(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "USER", "AGENT", "MERCHANT", "SYSTEM", "BOT" -> true;
            default -> false;
        };
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
