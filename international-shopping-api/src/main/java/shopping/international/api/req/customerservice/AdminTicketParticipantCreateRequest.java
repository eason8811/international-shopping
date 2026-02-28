package shopping.international.api.req.customerservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import shopping.international.domain.model.enums.customerservice.TicketParticipantRole;
import shopping.international.domain.model.enums.customerservice.TicketParticipantType;
import shopping.international.types.utils.Verifiable;

import static shopping.international.types.utils.FieldValidateUtils.require;
import static shopping.international.types.utils.FieldValidateUtils.requireNotNull;

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
    private TicketParticipantType participantType;
    /**
     * 参与用户 ID
     */
    @Nullable
    private Long participantUserId;
    /**
     * 参与方角色
     */
    @Nullable
    private TicketParticipantRole role;

    /**
     * 对新增工单参与方参数进行校验与规范化
     */
    @Override
    public void validate() {
        requireNotNull(participantType, "participantType 不能为空");

        if (participantUserId != null)
            require(participantUserId >= 1, "participantUserId 必须大于等于 1");

        requireNotNull(role, "role 不能为空");

        if (participantType.requiresUserId())
            require(participantUserId != null, "当前 participantType 需要提供 participantUserId");
    }
}
